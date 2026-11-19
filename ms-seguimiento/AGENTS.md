# AGENTS.md — ms-seguimiento

Leé primero el `AGENTS.md` de la raíz y `docs/07-estandar-microservicio.md`.

## Responsabilidad
Calendario de la comisión, asistencia, repositorio de GitHub vinculado, snapshots de actividad por sprint y **dashboard por equipo** (compone datos propios con los de `ms-catedra` y `ms-documentacion`).

## Datos
Puerto dev **8084** · esquema **`seguimiento`** (usuario `seguimiento_user`) · tablas `evento`, `asistencia`, `repositorio_vinculado`, `snapshot_actividad`, `actividad_integrante` (`docs/05`).

## Qué implementa
- Endpoints públicos **S01–S10** (`docs/06-contratos-api.md`). No expone `/internal`.
- Reglas: RN12, RN13, RN15, RN25–RN28, RN31, RN33 (`docs/03`).
- Funcionalidades: F11–F16.

## Depende de
- `ms-catedra` (`CatedraClient`: I01, I02, I03, I04) y `ms-documentacion` (`DocumentacionClient`: I06, I07), con el JWT del usuario o, en la tarea nocturna, un token de servicio (cliente `atenas-seguimiento-job`, rol `SERVICIO`).
- GitHub REST (`GitHubClient`, con `GITHUB_TOKEN`).

## Particularidades
- **Cálculo de actividad (RN26–RN28)**: para cada hito `SPRINT` del curso del equipo, contar commits de la rama elegida con fecha en `[fechaInicio, fechaLimite]` (los posteriores no cuentan), **sin merges** (más de un padre), atribuidos por `author.login` en minúsculas contra el usuario de GitHub cargado; los que no coinciden van a `commits_sin_atribuir`. Invariante: `commits_totales = Σ commits + commits_sin_atribuir`. Guardar una fila por integrante incluso con 0.
- `GitHubClient` oculta la **paginación** (bucle) y devuelve un modelo propio; traduce 404 → repositorio/rama inexistente, y fallas/respuestas inesperadas → estados de RN28.
- Fallas: el último snapshot pasa a `DESACTUALIZADO` conservando datos, o se crea uno `NO_DISPONIBLE` con contadores en `NULL`. Nunca bloquea y una falla en un equipo no corta a los demás.
- Marcas "sin actividad" y "baja contribución" se **calculan al consultar** (umbral configurable `atenas.actividad.umbral-baja-contribucion`, por defecto 0.5 del promedio). No se persisten.
- **Tarea nocturna** en `tarea/`: `@Scheduled` con cron configurable y `zone = America/Argentina/Cordoba`, por defecto 02:00.
- Asistencia: una fila por alumno y evento (`UNIQUE(evento_id, estudiante_sub)`); repetir actualiza; la carga por equipo se expande a filas de alumnos vigentes; `equipo_id` guarda el equipo al registrar. Sin fechas futuras.
- Dashboard: si falla `ms-catedra` → 503; si falla `ms-documentacion` → 200 con `entregas.disponible = false` (RN33).

## Tests propios
Unitarios de `service` (cálculo de actividad con casos de merges, commits fuera de ventana y sin atribuir; asistencia; degradación), `GitHubClient` con `MockRestServiceServer` (paginación, 404, 403 por cuota, 5xx, respuesta inesperada), `@WebMvcTest` por controlador.

## Este servicio NO
Guarda personas ni entregas; ni usa más de una rama por equipo; ni repositorios privados.
