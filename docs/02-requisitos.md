# 02 — Requisitos

## A. Requisitos de la cátedra vs. decisiones del equipo
Los requisitos salen del enunciado (E), el documento de alcance (A) y la rúbrica (R). Lo demás son decisiones nuestras.

| Requisito de la cátedra | Fuente | Cómo lo cumplimos | Ver |
|---|---|---|---|
| ≥ 2 microservicios funcionales en Java + Spring Boot, con responsabilidades coherentes (no CRUDs arbitrarios) | E | 3 servicios con dominios diferenciados y reglas propias | 04 |
| Al menos un proceso significativo con colaboración real entre servicios | E | Entrega verificada y dashboard | 04, 09 |
| API Gateway como punto de entrada, con Security Filter | E | Spring Cloud Gateway WebFlux | 04 |
| Keycloak (OIDC, JWT, roles) + Spring Security, autorización por rol e integración con el Gateway | E | Validación en dos capas | 04 |
| Definir y justificar la trusted zone | E | Solo Gateway y Keycloak publican puertos; `/internal/**` | 04 |
| ≥ 1 API pública externa que aporte información necesaria al negocio | E | Google Drive y GitHub | 04 |
| Qué pasa cuando la API externa no responde o devuelve algo inesperado | E | RN22, RN28, RN30 | 03 |
| JAR Maven reutilizable, instalado en `.m2` y usado por ≥ 2 servicios; justificar qué se comparte y qué no | E | `atenas-commons` | 04, adr |
| DBMS relacional; justificar la estrategia | E | Una instancia, esquema y usuario por servicio | 05, adr |
| APIs REST: métodos y códigos correctos, JSON, validación, errores, OpenAPI/Swagger fiel a lo implementado | E | springdoc + `ProblemDetail` | 06, 07 |
| Contenerización y Docker Compose reproducible; el C2 debe coincidir con lo desplegado | E | `docker-compose.yml` | 11 |
| Diagramas C1 y C2 (C4) que reflejen lo implementado | E | Mermaid en 04 | 04 |
| MVP con proceso central de punta a punta; el compromiso es la referencia de evaluación; cambios significativos se comunican | A | `14` registra cambios | 14 |
| Cualquier integrante puede explicar decisiones y funcionamiento; IA no exime de entender | E, A | Guía de defensa | 12 |
| Colección Postman/Bruno que se prueba y explica en la defensa | R (API-01) | `postman/` | 11 |
| Evidencias de la rúbrica (matriz de permisos, trazabilidad, secuencia, tests, README, secretos fuera del código) | R | Ver trazabilidad | 13 |

## B. Funcionalidades
Prioridad: **A** = imprescindible para el proceso central; **M** = media.

| ID | Funcionalidad | Actor | Prio |
|---|---|---|---|
| F01 | Autenticación y acceso por rol (JWT de Keycloak vía Gateway) | Todos | A |
| F02 | Gestionar estructura académica (cursos lectivos, comisiones, comisiones lectivas) | Jefe | A |
| F03 | Gestionar usuarios (alta/baja lógica de docentes y estudiantes, con cuenta y clave temporal) | Jefe | A |
| F04 | Conformar equipos (crear espacios, ingresar, salir, rechazar, asegurar, desasegurar) | Docente, alumnos | A |
| F05 | Cargar usuario de GitHub | Alumno | A |
| F06 | Definir hitos con ventana de fechas y fecha límite | Jefe | A |
| F07 | Registrar entrega con enlace de Drive verificado | Representante | A |
| F08 | Consultar entregas y su estado (incluye pendientes) | Docente, jefe, alumno | A |
| F09 | Abrir entrega (enlace + advertencia de cambios posteriores) | Docente | A |
| F10 | Corregir y calificar | Docente | A |
| F11 | Vincular repositorio público y rama de seguimiento | Representante | A |
| F12 | Actualizar actividad de GitHub (nocturna o a pedido) | Tarea, docente, jefe | A |
| F13 | Consultar dashboard por equipo | Docente, jefe | A |
| F14 | Gestionar calendario (clases y presentaciones) | Docente; todos consultan | M |
| F15 | Registrar asistencia por alumno o por equipo | Docente | M |
| F16 | Consultar asistencia | Alumno, docente, jefe | M |

Si falta tiempo, lo primero que se recorta es F14–F16 (prioridad media; se comunica a la cátedra). Nunca el proceso central (F01–F13).

## C. Requisitos no funcionales
| ID | Requisito | Criterio verificable |
|---|---|---|
| RNF-01 | Seguridad: toda la API exige JWT salvo la documentación OpenAPI; validación en Gateway y en cada servicio | 401/403 según 04 |
| RNF-02 | Resiliencia: timeouts en toda llamada saliente (conexión 2 s, lectura 5 s por defecto, configurables); 503 o degradación según la regla; sin reintentos ni circuit breaker | RN22, RN28, RN30, RN33 |
| RNF-03 | Trazabilidad: `X-Correlation-Id` generado en el Gateway, propagado y presente en cada línea de log (MDC) | Misma ID en logs de dos servicios |
| RNF-04 | Errores uniformes con `ProblemDetail` (RFC 7807) | Toda respuesta de error |
| RNF-05 | Reproducibilidad: `docker compose up --build` levanta todo desde cero | README |
| RNF-06 | OpenAPI refleja la API implementada | Swagger por servicio |
| RNF-07 | Calidad: cada historia entrega sus tests | `mvn clean test` verde |
| RNF-08 | Consistencia: todos los servicios siguen el estándar de 07 | Revisión de PR |
| RNF-09 | Fechas: UTC en base, offset de Argentina en la API | 06 |
| RNF-10 | Concurrencia: duplicados y sobrecupo impedidos por la base (restricciones únicas, `SELECT … FOR UPDATE`) | Postman |
