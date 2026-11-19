# AGENTS.md — api-gateway

Leé primero el `AGENTS.md` de la raíz y `docs/04-arquitectura.md` (sección Seguridad).

## Responsabilidad
Único punto de entrada. Enruta por prefijo, valida el JWT (firma, `iss`, vencimiento) y aplica **autorización gruesa** por método + ruta + rol. Genera el `X-Correlation-Id`.

## Datos
Puerto **8080 (publicado)**. No tiene base de datos. Es **reactivo** (Spring Cloud Gateway WebFlux + Spring Security reactivo): **no usa `atenas-commons`**.

## Qué implementa
- Rutas: `/api/catedra/**` → `ms-catedra`, `/api/documentacion/**` → `ms-documentacion`, `/api/seguimiento/**` → `ms-seguimiento`. Sin `StripPrefix`: las rutas se conservan.
- **Sin ruta para `/internal/**`** (debe responder 404) ni para `/actuator/**`.
- `SecurityWebFilterChain`: sin token → 401; rol no permitido → 403, según la columna **Gateway** de `docs/06-contratos-api.md`. Sin token solo: `/api/*/v3/api-docs/**` y Swagger UI.
- Conversor del JWT de Keycloak (`realm_access.roles` → `ROLE_*`) **propio** (reactivo).
- `GlobalFilter` que toma o genera `X-Correlation-Id` y lo propaga a los servicios y a la respuesta.

## Configuración
`issuer-uri` (el `iss` que ve el cliente: `http://localhost:8081/realms/atenas`) y `jwk-set-uri` (de dónde se bajan las claves: `http://keycloak:8080/…` en Docker, `localhost:8081` en dev). Las URIs de los servicios por perfil (`dev`: `localhost:8082-8084`; `docker`: `http://ms-<x>:8080`).

## Tests propios
`WebTestClient` con `spring-security-test` (`mockJwt`): 401 sin token, 403 con rol incorrecto, paso con rol correcto, 404 en `/internal/**`.

## Este servicio NO
Aplica reglas de dominio ni expone endpoints propios de negocio.
