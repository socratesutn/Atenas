# 13 — Trazabilidad con la rúbrica

Rúbrica general TPI 2026 (propuesta 0.11, 105 puntos; todavía no aprobada). Esta tabla se completa a medida que avanza el desarrollo: la columna **Estado** pasa de ☐ a ☑ cuando la evidencia existe y fue verificada.

## A. Criterio → evidencia
| Criterio (pts) | Qué hay que poder mostrar | Evidencia / dónde | Estado |
|---|---|---|---|
| **NEG-01** Proceso central del MVP (15) | Hito → entrega verificada → corrección → dashboard, de punta a punta | Postman carpeta 04; `docs/09` flujos 1 y 2 | ☐ |
| **NEG-02** Reglas de negocio (10) | Reglas implementadas, trazables a pruebas y a casos Postman; rechazos con código correcto | `docs/03`; tests con `@DisplayName("RN-xx …")`; Postman 06–07 | ☐ |
| **ARQ-01** Arquitectura y microservicios (12) | Límites coherentes, datos propios, independencia razonable, alternativas consideradas | `docs/04`, `docs/05`, `docs/adr.md` | ☐ |
| **INT-01** Integración y colaboración (12) | Qué servicio inicia, qué necesita, qué conserva, qué pasa si no responde; contratos; trazabilidad por correlation-id | `docs/09`, `docs/06` (internos), Postman 08, logs por `X-Correlation-Id` | ☐ |
| **SEG-01** Seguridad y acceso (8) | Matriz de permisos, validación en dos capas, trusted zone, secretos fuera del código, un acceso permitido y uno rechazado | `docs/06` (columnas Gateway/Dominio), `docs/04` seguridad, `.env.example`, Postman 07 | ☐ |
| **TEC-01** Calidad técnica (10) | Estructura estándar, validación, errores uniformes, tests significativos | `docs/07`, `docs/08`, `mvn clean test` verde en cada servicio | ☐ |
| **INF-01** Persistencia, reutilización y despliegue (10) | Esquemas por servicio, `atenas-commons` justificado, Compose reproducible desde cero, README | `docs/05`, `docs/04`, `docker-compose.yml`, `README.md` | ☐ |
| **ALC-01** Cumplimiento del alcance (8) | Cada funcionalidad comprometida → implementación → evidencia; cambios comunicados | Tabla B de este archivo; `docs/14` | ☐ |
| **DES-01** Complejidad y desafío (5) | Reglas ricas, integraciones reales (Drive, GitHub, Keycloak Admin), State, degradación controlada, concurrencia | `docs/03`, `docs/10`, `docs/adr.md` | ☐ |
| **PRE-01** Presentación y defensa (5) | Demo fluida; todos explican | `docs/12` | ☐ |
| **API-01** Colección de endpoints y pruebas (5) | Colección ejecutable y explicable, con positivos, negativos y fallas | `postman/` | ☐ |
| **DOC-01** Apreciación docente global (5) | Proceso iterativo y decisiones fundadas | `docs/adr.md`, `docs/14` | ☐ |

## B. Funcionalidad comprometida → implementación → evidencia
| F | Servicio | Endpoints | Postman | Tests | Estado |
|---|---|---|---|---|---|
| F01 | Gateway + todos | Seguridad de toda la API | 07 | `@WebMvcTest` 401/403 por controlador; tests del Gateway | ☐ |
| F02 | `ms-catedra` | C01–C06 | 01 | | ☐ |
| F03 | `ms-catedra` | C07–C13 | 01, 06 | | ☐ |
| F04 | `ms-catedra` | C16–C22 | 02, 06 | | ☐ |
| F05 | `ms-catedra` | C14, C15 | 02 | | ☐ |
| F06 | `ms-documentacion` | D01, D02 | 04 | | ☐ |
| F07 | `ms-documentacion` | D03 | 04, 07 | | ☐ |
| F08 | `ms-documentacion` | D02, D04, D05 | 04, 05 | | ☐ |
| F09 | `ms-documentacion` | D06 | 04, 08 | | ☐ |
| F10 | `ms-documentacion` | D07 | 04, 07 | | ☐ |
| F11 | `ms-seguimiento` | S07 | 03, 07 | | ☐ |
| F12 | `ms-seguimiento` | S08 + tarea nocturna | 04, 08 | | ☐ |
| F13 | `ms-seguimiento` | S09, S10 | 04, 08 | | ☐ |
| F14 | `ms-seguimiento` | S01, S02 | 03 | | ☐ |
| F15 | `ms-seguimiento` | S03–S05 | 03 | | ☐ |
| F16 | `ms-seguimiento` | S05, S06 | 05 | | ☐ |

## C. Comprobaciones del cuestionario (se ejecutan en la defensa)
1. Sin token → 401 del Gateway; el servicio no registra el pedido.
2. Hito definido por el jefe con curso y fechas válidos → 201; mismo `correlationId` en logs de `ms-documentacion` y `ms-catedra`.
3. Entrega verificada → 201 `ENTREGADA`; repetida → 409.
4. Apertura (cambios en Drive) y corrección con nota → `CORREGIDA`; repetir → 409.
5. Actividad de GitHub → snapshot `ACTUALIZADO`; con token inválido → `DESACTUALIZADO`.
6. Dashboard consolidado; con `ms-documentacion` pausado → sin sección de entregas.
7. Alumno no representante: 403 del Gateway al crear un hito y 403 del servicio al entregar.
8. `docker compose pause ms-catedra` → 503 con `ProblemDetail`; al reanudar → 200.
