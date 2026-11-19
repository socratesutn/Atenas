# 12 — Guía de defensa

La cátedra evalúa que **cualquiera** del equipo explique las decisiones y el funcionamiento, y que se pueda justificar *por qué* y *qué alternativas se consideraron*. Esta guía dice qué estudiar y qué se suele preguntar.

## Qué estudiar según el tema
| Tema | Apuntes | Por qué importa |
|---|---|---|
| Microservicios, límites, Gateway | 1, 19 | Justificar la división en tres servicios y el rol del Gateway |
| JWT, OIDC, Resource Server, roles | 20, 21-Seguridad | Explicar la validación en dos capas y la trusted zone |
| Capas, DTO vs entidad, errores | 15, 17 | Defender la estructura estándar y `ProblemDetail` |
| JPA y persistencia | 13, 16 | Explicar esquemas, restricciones, `LAZY`, `FOR UPDATE`, baja lógica |
| REST y códigos | 14 | Defender 409 vs 422 vs 503, verbos, rutas |
| `RestClient` y fallas | 18 | Timeouts, `onStatus`, propagación de JWT y correlation-id |
| Docker y Compose | 21-Docker, 22 | Red interna, healthchecks, perfiles, `.env` |
| Logging y trazabilidad | 23 | MDC y `X-Correlation-Id` |
| Tests | 7, 17 | Qué se prueba en cada capa y por qué sin Testcontainers |
| Patrones | repo `BE-EXAMPLE-design-patterns` (`state/`, `adapter/`) | Justificar State y Adapter y descartar el resto |

## Cómo contar el sistema en 2 minutos
1. Problema: información dispersa y controles manuales. 2. Solución: backend que integra datos propios con evidencia de Drive y GitHub. 3. Tres servicios con límites claros y un Gateway. 4. Proceso central: hito → entrega verificada → corrección → dashboard. 5. Seguridad con Keycloak en dos capas. 6. Qué hacemos cuando algo externo falla.

## Preguntas probables y por dónde responderlas
| Pregunta | Idea de respuesta | Ver |
|---|---|---|
| ¿Por qué tres servicios y no dos o uno? | Cada uno es dueño de datos y reglas distintas: personas/equipos, entregas, evidencia/seguimiento. Las dependencias no tienen ciclos | 04, adr-002 |
| ¿Qué pasa si `ms-catedra` se cae? | 503 controlado en lo que lo necesita; el dashboard degrada si cae `ms-documentacion`. Sin circuit breaker: timeouts y respuestas controladas bastan para este alcance | 04, adr-004 |
| ¿Por qué comparten una instancia de PostgreSQL? | Esquema y usuario por servicio, sin FK entre esquemas: independencia de modelo sin multiplicar infraestructura | 05, adr-003 |
| ¿Qué comparte `atenas-commons` y qué no? | Solo comportamiento técnico; entidades, reglas y DTO quedan en cada servicio para no acoplarlos | 04, adr-006 |
| ¿Por qué el Gateway y el servicio validan el JWT? | Defensa en profundidad: el Gateway corta lo grueso, el servicio aplica reglas de dominio | 04, adr-005 |
| ¿Cómo evitan que un token de un alumno dado de baja funcione? | No se revoca; vida corta y validación contra `ms-catedra` en las operaciones | 04 |
| ¿Por qué `PENDIENTE` no se guarda? | Se calcula: así los equipos nuevos no requieren sincronización | 05, adr-010 |
| ¿Cómo garantizan una sola entrega por equipo y hito? | `UNIQUE(hito_id, equipo_id)` ante pedidos simultáneos → 409 | 05 |
| ¿Cómo evitan sobrecupo en un equipo? | `SELECT … FOR UPDATE` sobre la fila del equipo | 05 |
| ¿Por qué State en `Equipo` y no en `Entrega`? | En `Equipo` varias reglas (RN05, RN08–RN11, RN18) dependen del estado y State las concentra; `Entrega` tiene una sola restricción y alcanza con una guarda. Con solo 2 estados la ganancia es modesta: hay que decirlo con honestidad | 10, adr-009 |
| ¿Qué pasa si falla Keycloak al dar de alta? | Se revierte la transacción local y se responde 503; riesgo residual: cuenta huérfana si falla el commit | 09, adr-012 |
| ¿Cómo demuestran un rechazo de seguridad y quién lo hizo? | Casos de la colección Postman + logs por `X-Correlation-Id`: 401/403 de rol los responde el Gateway; 403 de dominio, el servicio | 04, 13 |
| ¿Por qué no Testcontainers? | No está en la cursada; lo propio de PostgreSQL se verifica con Docker y Postman | 08, adr-013 |
| ¿Qué cambió respecto de lo comprometido? | Ver el listado de cambios y por qué | 14 |

## Preparación del equipo
- Cada integrante debe poder explicar el proceso central **completo**, no solo su servicio.
- Ensayar la demo con la colección Postman en este orden: sin token (401) → rol no permitido (403 del Gateway) → regla de dominio (403 del servicio) → entrega → corrección → dashboard → falla controlada (`docker compose pause ms-catedra` → 503).
- Tener a mano `docs/13-trazabilidad-rubrica.md` para mostrar cada criterio con su evidencia.
