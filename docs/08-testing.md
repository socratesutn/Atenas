# 08 — Estrategia de testing

> Para entender el porqué: Apunte 7 (JUnit 5, Mockito, TDD) y Apunte 17 (testing por capas).

## Regla de oro
**Cada historia entrega los tests de lo que implementa.** No se acumula deuda de tests: si agregás un endpoint o una regla, el mismo PR trae su prueba. Terminado = `mvn clean test` en verde.

## Niveles
| Nivel | Herramienta | Qué verifica | Obligatorio |
|---|---|---|---|
| Unitario de `service` | `@ExtendWith(MockitoExtension.class)`, repositorios y clientes mockeados | **Reglas de negocio** (RN): caso feliz y cada rechazo | Sí |
| Controlador | `@WebMvcTest` + `MockMvc` + `spring-security-test` (`jwt()` con authorities) | Código HTTP, forma del `ProblemDetail`, 401 y 403 por rol, validación 400 | Sí, por endpoint |
| Repositorio | `@DataJpaTest` con H2 | Consultas propias (`@Query`, derivadas) | Solo si hay consultas propias |
| Estado (State) | Unitario puro, sin Spring | Cada transición válida e inválida | Sí |
| Cliente (Adapter) | Unitario con `MockRestServiceServer` | Traducción de 404/5xx/timeout y de respuestas inesperadas | Sí |
| Comportamiento específico de PostgreSQL | Docker + colección Postman | Índice único parcial, `SELECT … FOR UPDATE`, esquemas | Se verifica a mano/Postman, **no** con Testcontainers |

## Convenciones
- Clases `XTest` en el mismo paquete que lo probado. `@DisplayName` en español que **cita la regla**: `@DisplayName("RN-29: rechaza una segunda entrega del mismo equipo y actividad")`.
- Nombre de método: `deberia<Resultado>_cuando<Condición>`.
- Patrón Arrange / Act / Assert. `assertThrows` con la excepción concreta y, si aplica, verificación del ID de regla.
- Un test no depende de otro ni de datos de otra clase. Sin `Thread.sleep`.
- No se prueban getters, records ni mapeos triviales.

## Prioridad si falta tiempo
1. Reglas del proceso central: RN17–RN24, RN29, RN30.
2. Equipos y estados: RN05–RN11.
3. Degradación: RN22, RN28, RN33.
4. El resto de las RN.
Mínimo por servicio antes de la defensa: tests de las reglas de su proceso central y 401/403 de cada controlador.

## H2 en los tests (`application-test.yml`)
H2 no soporta todo lo de PostgreSQL; hay que configurarlo así:
- URL con `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS <esquema>`.
- `spring.jpa.hibernate.ddl-auto=create-drop` y `hibernate.default_schema=<esquema>`.
- **`spring.sql.init.mode=never`**: `data.sql` crea un índice parcial que H2 no soporta.
- Seguridad: en `@WebMvcTest` importar la `SecurityConfig` del servicio y usar `jwt().authorities(…)`; no hace falta Keycloak.

## Qué evidencia esto ante la cátedra
Rúbrica TEC-01 y NEG-02: reglas trazables a tests (el `@DisplayName` cita la RN), pruebas que cubren decisiones relevantes y rechazos. La colección Postman (API-01) cubre integración y fallas controladas.
