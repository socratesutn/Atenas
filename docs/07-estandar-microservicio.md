# 07 — Estándar de microservicio

> Para entender el porqué: Apunte 15 (Spring, IoC/DI, estereotipos), 16 (Spring Data), 17 (capas, DTO vs entidad, `ProblemDetail`), 13 (JPA), 18 (`RestClient`), 23 (logging, MDC).
> **Los tres servicios (y en lo que aplique, el Gateway) siguen esta estructura.** Una excepción se justifica y se anota en el `AGENTS.md` del servicio y en `adr.md`.

## Estructura de directorios
```
ms-<nombre>/
├── pom.xml
├── Dockerfile
├── AGENTS.md / CLAUDE.md
└── src/
    ├── main/java/ar/edu/utn/frc/atenas/<nombre>/
    │   ├── <Nombre>Application.java
    │   ├── config/        Seguridad, RestClient, OpenAPI, propiedades @ConfigurationProperties
    │   ├── controller/    REST públicos; controller/interno/ para /internal/**
    │   ├── service/       Reglas de negocio y transacciones
    │   ├── repository/    Interfaces Spring Data (JpaRepository)
    │   ├── entity/        Entidades JPA y enums persistidos
    │   ├── dto/           records de entrada y salida; dto/externo/ para DTO de integración
    │   ├── mapper/        Mapeo manual entidad <-> dto (clases finales con métodos estáticos)
    │   ├── client/        Interfaces (Adapter) y sus implementaciones con RestClient
    │   ├── exception/     Excepciones específicas que extienden las de commons
    │   ├── estado/        (solo si usa State, ver 10)
    │   └── tarea/         (solo ms-seguimiento) tareas @Scheduled
    ├── main/resources/
    │   ├── application.yml  application-dev.yml  application-docker.yml
    │   └── data.sql         (solo ms-catedra: índice único parcial)
    └── test/
        ├── java/...         Misma jerarquía de paquetes que main
        └── resources/application-test.yml
```
`<nombre>` ∈ `catedra`, `documentacion`, `seguimiento`. Raíz de paquetes de la librería: `ar.edu.utn.frc.atenas.commons`.

## Responsabilidad de cada capa
| Capa | Hace | No hace |
|---|---|---|
| `controller` | Recibe HTTP, `@Valid` sobre el request, delega, arma `ResponseEntity` con el código correcto | Lógica de negocio, acceso a datos, devolver entidades |
| `service` | Reglas de negocio, orquestación, transacciones, lanzar excepciones de dominio | Conocer HTTP (`ResponseEntity`, status) |
| `repository` | Consultas (derivadas o `@Query` JPQL) | Reglas |
| `entity` | Estado persistido y comportamiento propio simple | Anotaciones de Jackson, lógica de otros agregados |
| `dto` | Contrato JSON: `record` inmutables con Bean Validation | Lombok |
| `mapper` | Convertir entidad ↔ DTO | Reglas, acceso a datos |
| `client` | Hablar con otro servicio o sistema externo detrás de una interfaz | Reglas de negocio |
| `exception` | Casos de error propios | Lógica |

## Reglas de código
**Idioma.** Todo en español: clases de dominio (`Entrega`, `Equipo`), métodos, variables, constantes, mensajes, `@DisplayName`, comentarios, commits y documentación. Se conservan en inglés solo los **sufijos técnicos** convencionales: `Controller`, `Service`, `Repository`, `Mapper`, `Client`, `Config`, `Request`, `Response`, `Exception`. Ejemplos: `EntregaService`, `CrearEquipoRequest`, `EntregaYaRegistradaException`.

**Inyección.** Por constructor (`final` + constructor; con Lombok `@RequiredArgsConstructor` permitido en servicios y controladores). Nunca `@Autowired` en campos.

**Entidades.** Lombok solo acá: `@Getter @Setter @NoArgsConstructor @Builder` (y `@AllArgsConstructor` si hace falta). **No `@Data`** (su `equals/hashCode/toString` rompe con asociaciones LAZY). `@ManyToOne(fetch = LAZY)`; sin `@ManyToMany` (entidad intermedia); `@OneToMany` solo si se justifica. Enums con `@Enumerated(EnumType.STRING)`. Fechas `Instant`. Evitar N+1 con `JOIN FETCH`.

**DTO.** `record`. Request con anotaciones de `jakarta.validation` (`@NotBlank`, `@Min`…). Response: solo lo necesario; nunca exponer IDs internos ajenos al contrato. Las fechas de respuesta son `OffsetDateTime` convertidas con la utilidad de zona de commons.

**Transacciones.** `@Transactional` en métodos del `service` que escriben. Cuando una operación consulta a otro servicio o a Drive/GitHub, esas llamadas van **antes** de la transacción: el método orquestador no es transaccional y abre una con `TransactionTemplate` solo para persistir.

**Errores.** Validación de formato → Bean Validation (400). Reglas de negocio → el `service` lanza una excepción de commons o una específica que la extienda, con el ID de regla (`RN-18`). El manejador global de commons produce el `ProblemDetail`. Prohibido `catch (Exception)`; solo se captura lo que se sabe tratar. Los `client` traducen: 404 → `NoEncontradoException`; timeout, conexión, 5xx u otro 4xx inesperado → `ServicioNoDisponibleException` (Apunte 18, `onStatus`).

**Seguridad.** El `SecurityFilterChain` de `config/` define el rol por método+ruta según `06`; `/actuator/health` y la documentación OpenAPI quedan sin token; `/internal/**` exige JWT. Las reglas de dominio (ser docente de la comisión, ser representante) van en el `service`, usando `UsuarioActual` de commons. Roles solo desde `Roles` de commons.

**Logging** (Apunte 23). `@Slf4j`. `controller`: entrada/salida en `INFO`; `service`: eventos de negocio en `INFO`/`DEBUG` y fallas en `WARN`/`ERROR`; `repository`: `TRACE`. El `correlationId` sale solo por MDC (patrón de log configurado en `application.yml`). **Nunca** loguear tokens, contraseñas, claves de API ni la clave temporal.

**OpenAPI** (springdoc). `@Tag` por controlador, `@Operation(summary = …)` en español y `@ApiResponse` para 4xx/5xx relevantes. Rutas: `springdoc.api-docs.path=/api/<nombre>/v3/api-docs`, `springdoc.swagger-ui.path=/api/<nombre>/swagger-ui.html`, y `springdoc.paths-to-match=/api/**` para excluir lo interno.

**Health.** `spring-boot-starter-actuator` con `management.endpoints.web.exposure.include=health,info`; los `healthcheck` de Compose consultan `/actuator/health`.

## Configuración
- `application.yml`: lo común (nombre, JPA, Jackson, logging, springdoc, `atenas.*`). `application-dev.yml` y `application-docker.yml`: lo que cambia por entorno (puerto, URLs de otros servicios, base de datos, `jwk-set-uri`). `application-test.yml` en `src/test/resources`.
- **Cero valores secretos en los YAML**: `${VARIABLE}` leídas del entorno. Las propiedades propias viven bajo el prefijo `atenas.` y se leen con `@ConfigurationProperties`.
- JPA: `ddl-auto=update`, `hibernate.default_schema=<esquema>`, `hibernate.jdbc.time_zone=UTC`, `open-in-view=false`. En `ms-catedra`: `spring.jpa.defer-datasource-initialization=true` y `spring.sql.init.mode=always`.
- JWT: `issuer-uri` solo se compara con el `iss`; las claves salen de `jwk-set-uri` (ver `11`).
- Timeouts: `atenas.http.timeout-conexion=2s`, `atenas.http.timeout-lectura=5s`.
- Zona horaria: `atenas.zona-horaria=America/Argentina/Cordoba`.

## Dependencias aprobadas (agregar otra requiere justificarlo)
| Servicio | Dependencias |
|---|---|
| Los 3 microservicios | `spring-boot-starter-web`, `-data-jpa`, `-validation`, `-security`, `-oauth2-resource-server`, `-actuator`; `postgresql` (runtime); `lombok`; `springdoc-openapi-starter-webmvc-ui`; `atenas-commons`. Test: `spring-boot-starter-test`, `spring-security-test`, `h2` |
| `ms-catedra`, `ms-seguimiento` | además `spring-boot-starter-oauth2-client` (Client Credentials hacia Keycloak) |
| `api-gateway` | `spring-cloud-starter-gateway-server-webflux`, `spring-boot-starter-security`, `-oauth2-resource-server`, `-actuator`. Test: `spring-boot-starter-test`, `spring-security-test` |
| `atenas-commons` | Spring (web, security, oauth2-resource-server, validation) con alcance `provided`, `lombok`, `slf4j`. Test: `spring-boot-starter-test` |

Versiones: Java 21, Spring Boot 3.5.x, Spring Cloud 2025.0.x, Maven 3.9, PostgreSQL 16, Keycloak 26.

## Dockerfile estándar (JAR prebuild)
```dockerfile
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
(`curl` es para el `healthcheck` de Compose.) El JAR del servicio ya incluye `atenas-commons`: la imagen no necesita el `.m2`.

## Qué es común y qué varía
| Común (no se cambia sin acordarlo) | Varía según el servicio |
|---|---|
| Estructura de paquetes y capas, idioma, naming | Entidades, reglas, DTO, clientes concretos |
| Manejo de errores (`ProblemDetail`), correlation-id, timeouts | Decisión 503 vs degradar |
| Perfiles `dev`/`docker`/`test`, `.env`, health, OpenAPI | `SecurityFilterChain` y permisos por endpoint |
| Dependencias aprobadas, estrategia de tests | `data.sql` (solo `ms-catedra`), `tarea/` (solo `ms-seguimiento`), `estado/` (solo si hay State) |
