# 09 — Flujos principales

> Los números de regla (RN) están en `03`; los endpoints, en `06`.

## 1. Registrar una entrega (D03) — colaboración `ms-documentacion` ↔ `ms-catedra` + Drive
```mermaid
sequenceDiagram
    actor R as Representante
    participant GW as API Gateway
    participant DOC as ms-documentacion
    participant CAT as ms-catedra
    participant DR as Google Drive
    participant DB as BD documentacion
    R->>GW: POST /api/documentacion/hitos/{id}/entregas (JWT)
    GW->>GW: valida firma, iss, vencimiento y rol ALUMNO
    GW->>DOC: reenvía con X-Correlation-Id
    DOC->>DOC: busca el hito (404 si no existe)
    DOC->>CAT: I03 estudiante por sub, I01 equipo
    alt ms-catedra no responde (timeout/5xx)
        DOC-->>R: 503 ProblemDetail
    end
    DOC->>DOC: RN18 representante, equipo asegurado, hito de su curso
    DOC->>DB: ¿ya existe entrega (hito, equipo)?
    alt existe
        DOC-->>R: 409 (RN29)
    end
    DOC->>DR: verifica archivo (existe, accesible, PDF o Google Doc)
    alt Drive no responde
        DOC-->>R: 503, no registra (RN22)
    else archivo inválido
        DOC-->>R: 400 enlace mal formado / 422 archivo no válido (RN19)
    end
    DOC->>DB: guarda ENTREGA + ARCHIVO_VERIFICADO (una transacción)
    DOC-->>R: 201 ENTREGADA, tardia según fecha límite (RN21)
```
Las consultas a `ms-catedra` y Drive ocurren **antes** de la transacción. Si dos pedidos llegan a la vez, `UNIQUE(hito_id, equipo_id)` rechaza al segundo (409).

## 2. Dashboard con degradación (S10)
```mermaid
sequenceDiagram
    actor D as Docente
    participant GW as API Gateway
    participant SEG as ms-seguimiento
    participant CAT as ms-catedra
    participant DOC as ms-documentacion
    D->>GW: GET /api/seguimiento/dashboard/equipos/{id}
    GW->>SEG: reenvía (JWT validado)
    SEG->>CAT: I01 equipo + I04 docente asignado
    alt ms-catedra no responde
        SEG-->>D: 503
    end
    SEG->>SEG: asistencia y último snapshot (BD propia)
    SEG->>DOC: I07 entregas del equipo
    alt ms-documentacion no responde
        SEG-->>D: 200 con entregas.disponible=false (RN33)
    else responde
        SEG-->>D: 200 asistencia + entregas + actividad
    end
```

## 3. Alta de usuario con Keycloak (C07 / C11)
```mermaid
sequenceDiagram
    actor J as Jefe
    participant CAT as ms-catedra
    participant DB as BD catedra
    participant KC as Keycloak (Admin API)
    J->>CAT: POST /api/catedra/estudiantes
    CAT->>DB: valida legajo y mail únicos (409), INSERT + flush
    CAT->>KC: token de servicio (atenas-catedra-admin)
    CAT->>KC: crea usuario con clave temporal y cambio obligatorio
    alt Keycloak falla
        CAT->>DB: ROLLBACK
        CAT-->>J: 503 (RN03)
    end
    CAT->>KC: asigna rol ALUMNO
    CAT->>DB: guarda sub y COMMIT
    CAT-->>J: 201 usuario habilitado
```
Riesgo aceptado: si el `COMMIT` falla después de crear la cuenta, queda una cuenta huérfana en Keycloak.

## 4. Actualización de actividad de GitHub (tarea nocturna o S08)
```mermaid
sequenceDiagram
    participant T as Tarea @Scheduled / Docente
    participant SEG as ms-seguimiento
    participant CAT as ms-catedra
    participant DOC as ms-documentacion
    participant GH as GitHub API
    T->>SEG: dispara (cron en zona de Argentina, o POST de S08)
    SEG->>SEG: token de servicio si es la tarea (Client Credentials)
    loop por cada repositorio vinculado
        SEG->>CAT: I01 equipo (integrantes y usuarios de GitHub)
        SEG->>DOC: I06 hitos SPRINT del curso
        loop por cada sprint
            SEG->>GH: commits de la rama en [inicio, límite]
            alt todo responde
                SEG->>SEG: excluye merges, atribuye por usuario, guarda snapshot ACTUALIZADO
            else alguna dependencia falla
                SEG->>SEG: último snapshot pasa a DESACTUALIZADO (o crea NO_DISPONIBLE)
            end
        end
    end
```
Una falla en un equipo no corta el procesamiento de los demás.
