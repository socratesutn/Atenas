# 05 — Modelo de datos

> Para entender el porqué: Apunte 13 (JPA, `@ManyToOne` LAZY, evitar `@ManyToMany`, N+1) y Apunte 16 (Spring Data).

## Estrategia
- **Una instancia de PostgreSQL 16**, base `atenas`, con un **esquema y un usuario por servicio** (`catedra`, `documentacion`, `seguimiento`) más `keycloak`. Cada servicio solo accede a su esquema.
- **Sin claves foráneas entre esquemas.** Las referencias entre servicios son lógicas (`*_id`, `*_sub`) y se validan por API al escribir.
- IDs `BIGINT` autogenerados. Fechas en `timestamptz` (UTC) mapeadas a `Instant`.
- Hibernate genera las tablas (`ddl-auto=update`) con `hibernate.default_schema`. El índice único parcial de `INTEGRANTE` se crea con `data.sql` en `ms-catedra`.
- Sin contenido de archivos ni de commits: se guardan enlaces, metadatos y conteos.

## Esquema `catedra` (ms-catedra) — todas FK reales
```mermaid
erDiagram
    CURSO_LECTIVO ||--o{ COMISION_LECTIVA : "abre"
    COMISION ||--o{ COMISION_LECTIVA : "se dicta como"
    COMISION_LECTIVA ||--o{ ASIGNACION_DOCENTE : "tiene"
    DOCENTE ||--o{ ASIGNACION_DOCENTE : "recibe"
    COMISION_LECTIVA ||--o{ ESTUDIANTE : "cursa"
    COMISION_LECTIVA ||--o{ EQUIPO : "agrupa"
    EQUIPO ||--o{ INTEGRANTE : "incluye"
    ESTUDIANTE ||--o{ INTEGRANTE : "participa"
    ESTUDIANTE |o--o| EQUIPO : "representa"
    CURSO_LECTIVO {
        bigint id PK
        int anio UK
        date fecha_inicio
        date fecha_fin
    }
    COMISION {
        bigint id PK
        varchar nombre UK
    }
    COMISION_LECTIVA {
        bigint id PK
        bigint comision_id FK
        bigint curso_lectivo_id FK
    }
    DOCENTE {
        bigint id PK
        varchar nombre
        varchar apellido
        varchar mail UK
        varchar sub UK
        boolean activo
    }
    ASIGNACION_DOCENTE {
        bigint id PK
        bigint docente_id FK
        bigint comision_lectiva_id FK
    }
    ESTUDIANTE {
        bigint id PK
        varchar legajo UK "solo dígitos"
        varchar nombre
        varchar apellido
        varchar mail UK "derivado del legajo"
        varchar sub UK
        varchar usuario_github UK "minúsculas, NULL hasta cargarlo"
        bigint comision_lectiva_id FK
        boolean activo
    }
    EQUIPO {
        bigint id PK
        bigint comision_lectiva_id FK
        varchar nombre
        int cupo_minimo
        int cupo_maximo
        varchar estado "ABIERTO o ASEGURADO"
        bigint representante_id FK,UK "NULL si no tiene integrantes"
    }
    INTEGRANTE {
        bigint id PK
        bigint equipo_id FK
        bigint estudiante_id FK
        timestamptz fecha_ingreso
        timestamptz fecha_baja "NULL si vigente"
    }
```
Restricciones: `UNIQUE(anio)`; `fecha_fin > fecha_inicio`; `UNIQUE(comision_id, curso_lectivo_id)`; `UNIQUE(docente_id, comision_lectiva_id)`; `UNIQUE(comision_lectiva_id, nombre)` en equipo; `cupo_minimo ≥ 1 y ≤ cupo_maximo`; **índice único parcial** sobre `integrante(estudiante_id) WHERE fecha_baja IS NULL` (RN07).
`estado` ∈ `ABIERTO, ASEGURADO`.

Decisiones:
- `COMISION` (permanente) separada de `COMISION_LECTIVA` (su dictado en un año).
- Sin tabla para el jefe: existe solo en Keycloak.
- **Alta:** se inserta la fila, `flush` (detecta legajo/mail duplicados), se crea la cuenta en Keycloak, se completa `sub` y se confirma. Si Keycloak falla, se revierte. Riesgo aceptado: si falla el commit tras crear la cuenta, queda una cuenta huérfana en Keycloak.
- **Baja lógica** (`activo=false`) para no romper referencias lógicas de otros esquemas.
- **Historial de integrantes:** salir o ser rechazado completa `fecha_baja`; si vuelve, se crea una fila nueva.
- `representante_id` se guarda para consulta directa; lo calcula RN09 y el servicio garantiza que sea integrante vigente.
- Cupo: al ingresar se bloquea la fila del equipo con `SELECT … FOR UPDATE`.
- La base no verifica misma comisión ni cupo: lo hace el servicio (RN08).

## Esquema `documentacion` (ms-documentacion)
```mermaid
erDiagram
    HITO ||--o{ ENTREGA : "recibe"
    ENTREGA ||--|| ARCHIVO_VERIFICADO : "referencia"
    HITO {
        bigint id PK
        bigint curso_lectivo_id "ref. lógica a ms-catedra"
        varchar nombre
        varchar tipo "INFORME_INICIAL, SPRINT o DOCUMENTO_FINAL"
        int orden
        timestamptz fecha_inicio
        timestamptz fecha_limite
    }
    ENTREGA {
        bigint id PK
        bigint hito_id FK
        bigint equipo_id "ref. lógica a ms-catedra"
        varchar estado "ENTREGADA o CORREGIDA"
        boolean tardia
        timestamptz fecha_entrega
        varchar registrada_por_sub
        int nota "NULL hasta corregir, 1 a 10"
        timestamptz fecha_correccion
        varchar corregida_por_sub
        boolean modificado_post_entrega "NULL hasta corregir"
    }
    ARCHIVO_VERIFICADO {
        bigint id PK
        bigint entrega_id FK,UK
        varchar enlace
        varchar drive_file_id
        varchar nombre
        varchar mime_type "PDF o Google Doc"
        timestamptz modified_time_entrega
    }
```
Restricciones: `UNIQUE(hito_id, equipo_id)` (impide duplicados aun con pedidos simultáneos → 409); nota entre 1 y 10; `fecha_inicio < fecha_limite`.
Decisiones: `curso_lectivo_id`, `equipo_id` y los `_sub` no tienen FK (pertenecen a `ms-catedra` y Keycloak) y se validan por la API interna. `PENDIENTE` **no se persiste** (se calcula al consultar, así los equipos nuevos no requieren sincronización). `modified_time_entrega` no se sobrescribe; la comparación se calcula al abrir y se congela en `modificado_post_entrega` al corregir (queda `NULL` si Drive falla en ese momento). `ENTREGA` y `ARCHIVO_VERIFICADO` se guardan juntas en **una transacción local**, después de consultar `ms-catedra` y Drive.

## Esquema `seguimiento` (ms-seguimiento)
```mermaid
erDiagram
    EVENTO ||--o{ ASISTENCIA : "registra"
    REPOSITORIO_VINCULADO ||--o{ SNAPSHOT_ACTIVIDAD : "genera"
    SNAPSHOT_ACTIVIDAD ||--o{ ACTIVIDAD_INTEGRANTE : "detalla"
    EVENTO {
        bigint id PK
        bigint comision_lectiva_id "ref. lógica"
        varchar tipo "CLASE o PRESENTACION"
        timestamptz fecha
        varchar descripcion
    }
    ASISTENCIA {
        bigint id PK
        bigint evento_id FK
        varchar estudiante_sub
        bigint equipo_id "equipo al registrar, NULL si no tenía"
        boolean presente
        varchar registrada_por_sub
        timestamptz fecha_registro
    }
    REPOSITORIO_VINCULADO {
        bigint id PK
        bigint equipo_id UK "ref. lógica"
        varchar url
        varchar propietario
        varchar nombre
        varchar rama_seguimiento
        timestamptz fecha_validacion
    }
    SNAPSHOT_ACTIVIDAD {
        bigint id PK
        bigint repositorio_id FK
        bigint hito_id "sprint, ref. lógica a ms-documentacion"
        timestamptz fecha_consulta
        varchar estado "ACTUALIZADO, DESACTUALIZADO o NO_DISPONIBLE"
        int commits_totales "NULL si NO_DISPONIBLE"
        int commits_sin_atribuir "NULL si NO_DISPONIBLE"
    }
    ACTIVIDAD_INTEGRANTE {
        bigint id PK
        bigint snapshot_id FK
        varchar estudiante_sub
        varchar usuario_github "usuario usado al consultar"
        int commits "puede ser 0"
    }
```
Restricciones: `UNIQUE(evento_id, estudiante_sub)`; `UNIQUE(equipo_id)` en repositorio; `UNIQUE(snapshot_id, estudiante_sub)`; `commits ≥ 0`.
Decisiones: asistencia **una fila por alumno** (la carga por equipo se expande; repetir actualiza); sin fila = "no registrada", `presente=false` = ausencia registrada. Cada consulta exitosa crea un snapshot; el dashboard usa el más reciente por repositorio y sprint. Ante falla: el último pasa a `DESACTUALIZADO` conservando datos, o se crea uno `NO_DISPONIBLE`. Se guarda una fila por integrante incluso con 0 commits (distingue "sin actividad" de "sin datos"). Invariante: `commits_totales = Σ commits + commits_sin_atribuir`. Las marcas "sin actividad" y "baja contribución" **no se guardan**: se calculan al consultar.
