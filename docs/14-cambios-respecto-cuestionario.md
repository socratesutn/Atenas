# 14 — Aclaraciones, supuestos y pendientes respecto del cuestionario

El cuestionario del 4/11 es el compromiso con la cátedra y **se mantiene como modelo oficial**: 16 funcionalidades (F01–F16), reglas RN01–RN33, 39 endpoints `/api` y 7 `/internal`, DER de los tres esquemas.
Este archivo registra solo lo que el equipo **aclaró o ajustó** sobre ese modelo. Lo marcado "avisar" conviene comunicarlo a la cátedra.

## A. Decisiones de alcance confirmadas
| Tema | Decisión |
|---|---|
| Hitos (F06, RN17) | Los crea **únicamente el jefe de cátedra** a nivel de curso lectivo. No hay "actividades" por docente ni comisión |
| Estado del proyecto del equipo | **Fuera de alcance.** El diagrama de estados `StateMachineProyecto` (en idea, en proceso, pendiente de aprobación, aprobado, rechazado, cancelado) no se implementa. El equipo solo tiene `ABIERTO` y `ASEGURADO`. No existen F17 ni RN34–RN36 |
| Google Drive (RN19) | Se valida existencia, acceso público y formato (PDF o Google Doc). Que el enlace permita comentar es una **instrucción operativa** a los alumnos, no una validación |

## B. Aclaraciones y ajustes sobre reglas existentes
| # | Regla | Qué se aclaró | Avisar |
|---|---|---|---|
| A-01 | RN02 | El mail del estudiante se deriva del legajo (`{legajo}@sistemas.frc.utn.edu.ar`); el legajo es numérico y único | No |
| A-02 | RN03 | La cuenta se crea con una **clave temporal** de configuración (`CLAVE_TEMPORAL`) y cambio obligatorio en el primer ingreso; no se envían correos | No |
| A-03 | RN05 y RN09 | El cuestionario las contradecía (la baja cierra la participación, pero salir de un equipo asegurado es 409). Se resuelve: **un equipo `ASEGURADO` impide cualquier salida**, incluida la baja lógica; primero un docente debe reabrirlo | Sí (aclaración) |
| A-04 | RN10 | El **docente de la comisión puede asegurar siempre**; el representante solo si `cupoMinimo ≤ integrantes ≤ cupoMaximo` y todos tienen usuario de GitHub. El endpoint `PATCH …/aseguramiento` admite `ALUMNO` y `DOCENTE` | **Sí** (el cuestionario solo permitía al representante) |
| A-05 | RN11 | Reabrir un equipo que ya está `ABIERTO` responde 409 | No |
| A-06 | RN24 | La nota es **entera** de 1 a 10 | No |
| A-07 | RN27 | Los commits posteriores a la fecha límite del sprint no cuentan; la ventana es `[fechaInicio, fechaLimite]` del hito `SPRINT` | No |
| A-08 | Fechas | `timestamptz` (UTC) en base; la API responde con offset de Argentina; el cron usa `America/Argentina/Cordoba` | No |

## C. Ajustes técnicos
| # | Ajuste |
|---|---|
| T-01 | `postgres/init.sql` pasa a **`postgres/init.sh`** para leer las claves del `.env` sin versionarlas |
| T-02 | `ms-catedra` y `ms-seguimiento` agregan `spring-boot-starter-oauth2-client` para Client Credentials (dependencia aprobada, `07`) |
| T-03 | Parámetros de consulta definidos: `GET /hitos?cursoLectivoId=` (cualquier rol autenticado de los tres, sin regla adicional), `GET /entregas?comisionLectivaId=&hitoId=`, dashboard por `comisionLectivaId` (o `cursoLectivoId`, solo jefe) |
| T-04 | Rutas con `comisioneslectivas` (corte de línea del PDF del cuestionario) se normalizan a `comisiones-lectivas` |
| T-05 | La carpeta del Gateway se llama `api-gateway` |

## D. Supuestos propuestos (default adoptado; confirmar)
| # | Supuesto | Default |
|---|---|---|
| S-01 | Umbral de "baja contribución" (RN27) | 50 % del promedio de commits por integrante, configurable (`atenas.actividad.umbral-baja-contribucion`) |
| S-02 | Horario de la tarea nocturna (RN31) | 02:00, `America/Argentina/Cordoba`, configurable |
| S-03 | Si el representante necesita GitHub cargado para asegurar | Sí (RN10 del cuestionario); el docente no |

## E. Inconsistencias del cuestionario resueltas
- RN04, RN14, RN16 y RN32 no existen; el dashboard degradado es **RN33** (el cuestionario lo cita como RN-32 en dos lugares y como "RN-3" en uno).
- "Todo endpoint responde 401 sin token" vs documentación OpenAPI pública: la documentación OpenAPI es la única excepción.
- Contradicción RN05/RN09: ver A-03.

## F. Pendientes abiertos
- Decidir si se comunica A-03 y A-04 a la cátedra (modifican levemente reglas comprometidas).
- Spike del día 1: `iss` de Keycloak, healthcheck de Keycloak, Swagger por el Gateway, Drive y GitHub (ver `11`).
