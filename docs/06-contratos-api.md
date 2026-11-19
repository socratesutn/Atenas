# 06 — Contratos de API

> Para entender el porqué: Apunte 14 (REST, verbos, códigos, OpenAPI) y Apunte 17 (`ResponseEntity`, `ProblemDetail`). El contrato ejecutable es el OpenAPI de cada servicio; este archivo es la fuente de diseño y debe coincidir con él.

## Convenciones
- Base: `/api/{catedra|documentacion|seguimiento}/…` (por el Gateway) y `/internal/…` (solo entre servicios). Sustantivos en plural y minúscula con guiones. El Gateway **no** reescribe rutas: los controladores ya llevan el prefijo.
- JSON con propiedades en `camelCase` y español (`comisionLectivaId`). Todo **request** es un `record` con Bean Validation; todo **response**, un `record`.
- Fechas: ISO-8601 con offset de Argentina (`2026-11-18T10:00:00-03:00`). Se aceptan con cualquier offset.
- Códigos de éxito: `POST` que crea → **201** + `Location`; `GET` → **200**; `PUT`/`PATCH` de acción → **200** con el recurso; `DELETE` → **204**.
- Sin paginación. Filtros por query string (`comisionLectivaId`, `hitoId`, `cursoLectivoId`, `equipoIds`).
- Identificadores de personas en rutas y cuerpos de otros servicios: `sub` de Keycloak.
- Documentación: `/api/{servicio}/v3/api-docs` y Swagger UI sin token. Los controladores internos **no** aparecen en OpenAPI.

## Errores (`ProblemDetail`, RFC 7807)
| Código | Cuándo | Excepción base (commons) |
|---|---|---|
| 400 | Validación de entrada, formato inválido | `SolicitudInvalidaException` |
| 401 | Sin token / inválido / vencido (lo responde el Gateway; el servicio también) | — |
| 403 | Rol o regla de dominio no cumplida | `AccesoDenegadoException` |
| 404 | Un identificador no existe | `NoEncontradoException` |
| 409 | Conflicto de estado o duplicado | `ConflictoException` |
| 422 | Dato sintácticamente válido pero rechazado (nota fuera de rango, archivo no válido) | `NoProcesableException` |
| 503 | Dependencia no responde y la operación no puede continuar | `ServicioNoDisponibleException` |

```json
{
  "type": "about:blank",
  "title": "Conflicto",
  "status": 409,
  "detail": "Ya existe una entrega del equipo 7 para el hito 3",
  "instance": "/api/documentacion/hitos/3/entregas",
  "timestamp": "2026-11-18T10:15:30-03:00",
  "regla": "RN-29",
  "correlationId": "0b7e5c9e-…",
  "errores": [ { "campo": "enlace", "mensaje": "no debe estar vacío" } ]
}
```
`regla` (ID de RN) aparece cuando el rechazo proviene de una regla; `errores` solo en 400 de validación.

## Roles
`JEFE_CATEDRA`, `DOCENTE`, `ALUMNO`, `SERVICIO`. Columna **Gateway** = roles que pasan el Gateway (y el `SecurityFilterChain` del servicio). Columna **Dominio** = regla que valida el servicio (→ 403/404/409).

## ms-catedra (`/api/catedra`)
| ID | Método y ruta | Operación | Gateway | Dominio | RN |
|---|---|---|---|---|---|
| C01 | `POST /cursos-lectivos` | Crear curso lectivo | JEFE | — | — |
| C02 | `GET /cursos-lectivos` | Listar cursos lectivos | JEFE, DOCENTE | — | — |
| C03 | `POST /comisiones` | Crear comisión | JEFE | — | — |
| C04 | `GET /comisiones` | Listar comisiones | JEFE | — | — |
| C05 | `POST /cursos-lectivos/{id}/comisiones-lectivas` | Habilitar comisión en un curso | JEFE | — | — |
| C06 | `GET /cursos-lectivos/{id}/comisiones-lectivas` | Listar comisiones de un curso | JEFE, DOCENTE | Docente: solo las suyas | RN01 |
| C07 | `POST /docentes` | Registrar docente y crear su cuenta | JEFE | — | RN03 |
| C08 | `GET /docentes` | Listar docentes | JEFE | — | — |
| C09 | `PATCH /docentes/{id}/baja` | Baja lógica de docente | JEFE | — | RN05 |
| C10 | `POST /comisiones-lectivas/{id}/docentes` | Asignar docente a una comisión | JEFE | — | — |
| C11 | `POST /estudiantes` | Registrar estudiante en una comisión y crear su cuenta | JEFE | — | RN02, RN03 |
| C12 | `GET /comisiones-lectivas/{id}/estudiantes` | Listar estudiantes de una comisión | JEFE, DOCENTE | Docente de la comisión | RN01 |
| C13 | `PATCH /estudiantes/{id}/baja` | Baja lógica de estudiante | JEFE | Equipo `ABIERTO` | RN05 |
| C14 | `GET /mi-perfil` | Datos propios, comisión, curso y equipo | ALUMNO | `sub` del token | RN15 |
| C15 | `PUT /mi-perfil/usuario-github` | Cargar usuario de GitHub | ALUMNO | `sub` del token | RN26 |
| C16 | `POST /comisiones-lectivas/{id}/equipos` | Crear espacio de equipo | DOCENTE | Docente de la comisión | RN06 |
| C17 | `GET /comisiones-lectivas/{id}/equipos` | Listar equipos y disponibilidad | JEFE, DOCENTE, ALUMNO | Docente o alumno de la comisión | RN01 |
| C18 | `GET /equipos/{id}` | Consultar equipo, integrantes y representante | JEFE, DOCENTE, ALUMNO | Docente o alumno de la comisión | RN01 |
| C19 | `POST /equipos/{id}/integrantes` | Ingresar a un equipo | ALUMNO | Alumno de la comisión; `ABIERTO`; cupo | RN07, RN08, RN09 |
| C20 | `DELETE /equipos/{id}/integrantes/{estudianteId}` | Abandonar el equipo o rechazar un ingreso | ALUMNO | El propio integrante o el representante; `ABIERTO` | RN09 |
| C21 | `PATCH /equipos/{id}/aseguramiento` | Asegurar el equipo | ALUMNO, DOCENTE | Representante con cupo en rango y GitHub cargado, **o** docente de la comisión | RN10 |
| C22 | `PATCH /equipos/{id}/reapertura` | Reabrir el equipo (vuelve a `ABIERTO`) | DOCENTE | Docente de la comisión | RN11 |

## ms-documentacion (`/api/documentacion`)
| ID | Método y ruta | Operación | Gateway | Dominio | RN |
|---|---|---|---|---|---|
| D01 | `POST /hitos` | Definir un hito con ventana de fechas y fecha límite | JEFE | Curso lectivo existente; fechas dentro del curso (consulta a `ms-catedra`) | RN17 |
| D02 | `GET /hitos?cursoLectivoId=` | Listar los hitos de un curso | JEFE, DOCENTE, ALUMNO | — | — |
| D03 | `POST /hitos/{id}/entregas` | Registrar entrega con enlace de Drive verificado | ALUMNO | Representante de equipo asegurado, hito de su curso | RN18–RN22, RN29 |
| D04 | `GET /entregas?comisionLectivaId=&hitoId=` | Listar estado de entregas por comisión e hito, incluidas pendientes | JEFE, DOCENTE | Docente de la comisión | RN01, RN20 |
| D05 | `GET /mi-equipo/entregas` | Entregas del equipo propio, incluidas pendientes | ALUMNO | `sub` del token | RN15, RN20 |
| D06 | `GET /entregas/{id}` | Abrir entrega: enlace y si cambió en Drive | DOCENTE | Docente de la comisión | RN30 |
| D07 | `PATCH /entregas/{id}/correccion` | Cargar nota y marcar corregida (`{"nota":8}`) | DOCENTE | Docente de la comisión; `ENTREGADA`; 1–10 | RN24 |

## ms-seguimiento (`/api/seguimiento`)
| ID | Método y ruta | Operación | Gateway | Dominio | RN |
|---|---|---|---|---|---|
| S01 | `POST /comisiones-lectivas/{id}/eventos` | Crear clase o presentación | DOCENTE | Docente de la comisión | RN12 |
| S02 | `GET /comisiones-lectivas/{id}/eventos` | Consultar calendario | JEFE, DOCENTE, ALUMNO | Docente o alumno de la comisión | RN01 |
| S03 | `PUT /eventos/{id}/asistencias/{estudianteSub}` | Registrar o corregir asistencia de un alumno | DOCENTE | Docente de la comisión | RN13 |
| S04 | `PUT /eventos/{id}/equipos/{equipoId}/asistencias` | Registrar o corregir asistencia de un equipo | DOCENTE | Docente de la comisión | RN13 |
| S05 | `GET /eventos/{id}/asistencias` | Consultar asistencia de un evento | JEFE, DOCENTE | Docente de la comisión | RN01 |
| S06 | `GET /mi-asistencia` | Asistencia propia | ALUMNO | `sub` del token | RN15 |
| S07 | `PUT /equipos/{id}/repositorio` | Vincular y validar repositorio y rama | ALUMNO | Representante | RN25 |
| S08 | `POST /equipos/{id}/actividad/actualizacion` | Actualizar a pedido la actividad de GitHub | JEFE, DOCENTE | Docente de la comisión | RN27, RN31 |
| S09 | `GET /dashboard/equipos?comisionLectivaId=` (o `cursoLectivoId=`, solo jefe) | Resumen de equipos | JEFE, DOCENTE | Docente de la comisión | RN33 |
| S10 | `GET /dashboard/equipos/{id}` | Asistencia, entregas y actividad de un equipo | JEFE, DOCENTE | Docente de la comisión | RN33 |

**Total: 39 endpoints `/api` + 7 `/internal`.**

## Payloads clave
| Endpoint | Request |
|---|---|
| C07 | `{ "nombre", "apellido", "mail" }` |
| C11 | `{ "legajo", "nombre", "apellido", "comisionLectivaId" }` (el mail se deriva) |
| C16 | `{ "nombre", "cupoMinimo", "cupoMaximo" }` |
| C15 | `{ "usuarioGithub" }` |
| D01 | `{ "cursoLectivoId", "nombre", "tipo", "orden", "fechaInicio", "fechaLimite" }` |
| D03 | `{ "enlace" }` |
| D07 | `{ "nota": 8 }` (entero 1–10) |
| S01 | `{ "tipo": "CLASE", "fecha", "descripcion" }` |
| S03 / S04 | `{ "presente": true }` |
| S07 | `{ "url", "rama" }` |

La respuesta de **D06** incluye `enlace`, `estado`, `tardia` y `modificadoPostEntrega` ∈ `{ "SI", "NO", "NO_VERIFICADO" }`.
La respuesta de **S10** tiene tres secciones: `asistencia`, `entregas` (con `disponible: false` si `ms-documentacion` falló) y `actividad` (con `estado` del snapshot y marcas informativas).

## Contratos internos (`/internal`, sin ruta en el Gateway)
Se llaman con el JWT del usuario propagado; los marcados **(SERVICIO)** también aceptan el rol `SERVICIO`. No requieren rol del Gateway: la regla de dominio la valida el **servicio que llama**.

| ID | Ruta | Provee | Lo usa |
|---|---|---|---|
| I01 | `GET /internal/equipos/{id}` **(SERVICIO)** | Equipo con estado, comisión, curso, representante e integrantes | `ms-documentacion`, `ms-seguimiento` |
| I02 | `GET /internal/equipos?comisionLectivaId=` o `?cursoLectivoId=` | Lista de equipos (mismo formato resumido) | `ms-seguimiento` |
| I03 | `GET /internal/estudiantes/por-sub/{sub}` | Comisión, curso y equipo vigentes del alumno | `ms-documentacion`, `ms-seguimiento` |
| I04 | `GET /internal/comisiones-lectivas/{id}/docentes/{sub}` | Si el docente está asignado a la comisión | `ms-documentacion`, `ms-seguimiento` |
| I05 | `GET /internal/cursos-lectivos/{id}` | Curso lectivo y sus fechas, para validar un hito | `ms-documentacion` |
| I06 | `GET /internal/hitos?cursoLectivoId=&tipo=SPRINT` **(SERVICIO)** | Hitos de un curso (para calcular la actividad por sprint) | `ms-seguimiento` |
| I07 | `GET /internal/entregas?cursoLectivoId=&equipoIds=1,2` | Estado y nota por equipo y hito (incluye `PENDIENTE` calculadas) | `ms-seguimiento` |

I01–I05 pertenecen a `ms-catedra`; I06–I07 a `ms-documentacion`. Los DTO de estas respuestas se **duplican** en cada consumidor con solo los campos que usa (ver ADR-006). Forma acordada (los consumidores ignoran propiedades desconocidas):

```jsonc
// I01 (I02 devuelve una lista con el mismo formato)
{ "id": 7, "nombre": "Equipo 1", "comisionLectivaId": 10, "cursoLectivoId": 2,
  "estado": "ASEGURADO", "asegurado": true, "representanteSub": "uuid",
  "integrantes": [ { "estudianteId": 5, "sub": "uuid", "legajo": "411626", "usuarioGithub": "usuario" } ] }
// I03
{ "estudianteId": 5, "sub": "uuid", "activo": true, "comisionLectivaId": 10, "cursoLectivoId": 2, "equipoId": 7 }  // equipoId puede ser null
// I04 -> 404 si la comisión no existe
{ "asignado": true }
// I05
{ "id": 2, "anio": 2026, "fechaInicio": "2026-03-01", "fechaFin": "2026-12-15" }
// I06
[ { "id": 3, "cursoLectivoId": 2, "nombre": "Sprint 1", "tipo": "SPRINT", "orden": 1, "fechaInicio": "…", "fechaLimite": "…" } ]
// I07
[ { "equipoId": 7, "hitoId": 3, "estado": "ENTREGADA", "tardia": false, "nota": null } ]
```
