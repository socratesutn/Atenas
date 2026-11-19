# AGENTS.md — atenas-commons

Leé primero el `AGENTS.md` de la raíz y la sección "Librería común" de `docs/04-arquitectura.md` (y ADR-006).

## Responsabilidad
Comportamiento **técnico transversal** reutilizable por `ms-catedra`, `ms-documentacion` y `ms-seguimiento`. **No contiene entidades, reglas de negocio ni DTO de negocio.** No es una aplicación (no se ejecuta). El Gateway no la usa.

## Coordenadas
`ar.edu.utn.frc.atenas:atenas-commons:1.0.0`. Se instala con `mvn clean install` y cada servicio la declara como dependencia. Los servicios la incluyen en `scanBasePackages`.

## Contenido (paquete raíz `ar.edu.utn.frc.atenas.commons`)
| Paquete | Clases |
|---|---|
| `error` | `ErrorDominioException` (base: status + id de regla), `SolicitudInvalidaException` (400), `AccesoDenegadoException` (403), `NoEncontradoException` (404), `ConflictoException` (409), `NoProcesableException` (422), `ServicioNoDisponibleException` (503), `ManejadorGlobalDeErrores` (`@RestControllerAdvice` → `ProblemDetail` con `timestamp`, `regla`, `correlationId`, `errores`) |
| `seguridad` | `Roles` (constantes `JEFE_CATEDRA`, `DOCENTE`, `ALUMNO`, `SERVICIO`), `ConversorJwtKeycloak` (`realm_access.roles` → `ROLE_*`), `UsuarioActual` (`sub`, roles, token) |
| `http` | Configuración base de `RestClient` con timeouts (`atenas.http.*`), traducción de fallas (conexión, timeout, 5xx → `ServicioNoDisponibleException`), interceptor de llamadas internas (propaga `Authorization` y `X-Correlation-Id`) |
| `traza` | `CorrelationIdFilter`: toma o genera `X-Correlation-Id`, lo pone en el MDC y en la respuesta |
| `tiempo` | Utilidad de zona horaria (`atenas.zona-horaria`) para convertir `Instant` ↔ `OffsetDateTime` |

## Reglas especiales
- Spring y Jakarta se declaran con alcance **`provided`** (cada servicio aporta las suyas, Spring Boot 3.5).
- Cambios acá afectan a todos: se avisa al equipo y se vuelve a ejecutar `mvn clean install`.
- **No** agregar clases de negocio, ni clientes concretos (Catedra, Drive, GitHub, Keycloak), ni `SecurityFilterChain`.
- Mantener el código mínimo: si algo lo usa un solo servicio, no va acá.

## Tests propios
Manejador de errores (cada excepción → status y campos del `ProblemDetail`), conversor de JWT, filtro de correlation-id (genera y respeta el recibido), traducción de fallas del cliente HTTP.
