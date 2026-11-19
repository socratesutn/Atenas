# Decisiones arquitectónicas (ADR)

Formato: **Contexto → Decisión → Alternativas → Consecuencias**. Las decisiones nuevas se agregan al final con el siguiente número. Estado: todas *aceptadas* salvo que se indique.

---
## ADR-001 — Microservicios con API Gateway; sin Eureka ni Config Server
**Contexto.** El enunciado exige microservicios y un Gateway. La cursada (Apunte 1) presenta además Service Discovery y Config centralizada.
**Decisión.** API Gateway con rutas estáticas por prefijo hacia los nombres DNS de Docker. Sin Eureka ni Spring Cloud Config.
**Alternativas.** Monolito modular (descartado: no cumple el enunciado). Eureka + Config Server (descartado: con 3 servicios fijos en Compose no aporta valor y agrega contenedores).
**Consecuencias.** Menos piezas que operar y defender. Si el sistema creciera, habría que reconsiderar el discovery.

## ADR-002 — Tres servicios por responsabilidad
**Contexto.** Hay que dividir por responsabilidades funcionales, no por capas ni por CRUD.
**Decisión.** `ms-catedra` (personas, estructura, equipos; fuente de verdad de pertenencia), `ms-documentacion` (hitos, entregas, notas, Drive), `ms-seguimiento` (calendario, asistencia, GitHub, dashboard). Dependencias sin ciclos: seguimiento → documentacion → catedra y seguimiento → catedra.
**Alternativas.** Dos servicios (mezclar entregas con seguimiento: dos fuentes externas distintas en un solo dueño). Cuatro o más (calendario/asistencia separado: más costo de integración sin ganancia de independencia).
**Consecuencias.** `ms-seguimiento` es el más dependiente y por eso compone el dashboard degradando si falta `ms-documentacion`.

## ADR-003 — Una instancia de PostgreSQL, esquema y usuario por servicio
**Contexto.** Se permite una instancia común, pero la rúbrica pide justificar si se comparten datos.
**Decisión.** Una instancia, base `atenas`, esquemas `catedra`, `documentacion`, `seguimiento` y `keycloak`, cada uno con su usuario. Sin claves foráneas entre esquemas; referencias lógicas validadas por API.
**Alternativas.** Base por servicio (más aislamiento pero 3–4 contenedores de BD sin beneficio funcional aquí). Esquema compartido sin separar (acoplamiento de modelos, descartado).
**Consecuencias.** Independencia de modelo con una sola infraestructura. Las referencias entre servicios no tienen integridad referencial: se compensa validando al escribir y con baja lógica.

## ADR-004 — Comunicación síncrona REST con timeouts; sin circuit breaker ni reintentos
**Contexto.** Hay colaboración real entre servicios y fallas externas posibles. Los resilience patterns avanzados no están en la cursada.
**Decisión.** `RestClient` con timeouts (conexión 2 s, lectura 5 s). Cada proceso decide: 503 cuando la operación no puede continuar, o datos degradados cuando sí. Sin reintentos automáticos ni circuit breaker.
**Alternativas.** Mensajería asíncrona (complejidad y fuera de la cursada). Resilience4j (más tecnología sin necesidad en este volumen).
**Consecuencias.** Comportamiento predecible y fácil de demostrar con `docker compose pause`. Una caída prolongada se refleja directo en los tiempos de respuesta hasta el timeout.

## ADR-005 — Seguridad en dos capas con trusted zone
**Contexto.** Hay que definir cómo se reparte la seguridad entre Gateway y servicios.
**Decisión.** El Gateway valida firma, emisor y vencimiento y aplica autorización gruesa por rol. Cada servicio vuelve a validar el JWT, aplica rol por endpoint y reglas de dominio. Solo Gateway y Keycloak publican puertos; `/internal/**` no tiene ruta en el Gateway. Las llamadas internas propagan el JWT del usuario; la tarea nocturna usa Client Credentials.
**Alternativas.** Seguridad solo en el Gateway (un servicio alcanzable queda desprotegido y no puede aplicar reglas de dominio). Seguridad solo en servicios (el Gateway no cumpliría su rol). mTLS entre servicios (fuera de alcance).
**Consecuencias.** Defensa en profundidad con lógica de roles duplicada en dos capas; el JWT no se revoca, por eso es de vida corta.

## ADR-006 — `atenas-commons`: solo lo técnico y transversal
**Contexto.** El JAR común es un requisito; hay que justificar qué se comparte y qué no.
**Decisión.** Se comparte: excepciones base y manejador de `ProblemDetail`, roles y conversor de JWT, cliente HTTP base con timeouts e interceptor, filtro de correlation-id, utilidad de zona horaria. No se comparte: entidades, repositorios, reglas, estados, DTO de negocio ni de integración, clientes concretos. Sin módulo de contratos compartido. El Gateway no la usa (WebFlux): duplica roles y conversor a mano.
**Alternativas.** Módulo de contratos compartido (obliga a recompilar y redesplegar consumidores ante cada cambio). Dividir la librería en módulo servlet y módulo reactivo (más piezas; se prefiere un solo JAR).
**Consecuencias.** Los DTO de integración se duplican en los consumidores a propósito. Distribución mínima por `.m2` local.

## ADR-007 — Monorepo, proyectos Maven independientes y JAR prebuild
**Contexto.** Cuatro personas en paralelo; el enunciado pide que `atenas-commons` se consuma como dependencia Maven instalada.
**Decisión.** Un repositorio con carpetas por proyecto, **sin parent pom** ni multi-módulo. Orden: `mvn clean install` en commons, `mvn clean package` en cada servicio, `docker compose up --build`. Cada Dockerfile copia el JAR de `target/` (que ya incluye commons).
**Alternativas.** Repos separados (más fricción de coordinación). Multi-módulo con Dockerfile multi-stage (arranque de un solo comando, pero se aparta del estándar de la cátedra, Apunte 21-Docker/22).
**Consecuencias.** Hace falta JDK 21 y Maven para levantar el sistema; el README lo documenta. Mantiene el mecanismo exacto que pide el enunciado.

## ADR-008 — Adapter para sistemas externos y otros servicios
**Decisión.** Toda integración se hace detrás de una interfaz en `client/` con implementación `RestClient`. Ver `10`.
**Alternativas.** Usar `RestClient` directo desde los `service` (acopla reglas a contratos externos y exige red para testear).
**Consecuencias.** Más clases pequeñas a cambio de aislamiento, errores traducidos en un solo lugar y tests sin red.

## ADR-009 — State solo para `Equipo`
**Contexto.** El equipo tiene dos estados (`ABIERTO`, `ASEGURADO`) y varias reglas cuya validez depende de ellos (RN05, RN08–RN11, RN18). `Entrega` y `Snapshot` tienen poco comportamiento atado al estado.
**Decisión.** Patrón State en `Equipo`: enum persistido + objeto de comportamiento por estado. `Entrega` y `Snapshot` usan enum con guardas simples.
**Alternativas.** Enum con `if` en el `service` (más simple, pero la lógica se repite en cada operación y crece con cada estado nuevo). State en todo (sobreingeniería).
**Consecuencias.** Con solo dos estados la ganancia es modesta; el equipo la acepta porque centraliza reglas y deja abierta la extensión. Si en la defensa se cuestiona, la respuesta honesta es esa. Se prueba con unitarios sin Spring.

## ADR-010 — `PENDIENTE` se calcula, no se persiste
**Decisión.** No existe fila de entrega hasta que se registra; `PENDIENTE` = hito sin entrega del equipo, calculado al consultar.
**Alternativas.** Crear filas pendientes al crear el hito (exige sincronizar con equipos nuevos o dados de baja).
**Consecuencias.** Consultas que combinan hitos y equipos (I07); sin riesgo de datos desincronizados.

## ADR-011 — Fechas en UTC con presentación en horario de Argentina
**Contexto.** Drive y GitHub devuelven UTC; la cátedra opera en horario de Argentina; "tardía" y la ventana del sprint dependen de la zona.
**Decisión.** `timestamptz`/`Instant` en base; la API responde con offset `-03:00`; el cron de la tarea nocturna usa `America/Argentina/Cordoba`.
**Alternativas.** `timestamp` sin zona (ambiguo y propenso a errores de comparación).
**Consecuencias.** Las comparaciones son siempre entre instantes; solo la presentación usa la zona.

## ADR-012 — Alta de cuentas por Admin API con clave temporal y compensación
**Contexto.** El jefe da de alta usuarios; la cuenta debe existir en Keycloak.
**Decisión.** `ms-catedra` inserta, hace `flush`, crea la cuenta con clave temporal y cambio obligatorio, asigna el rol y confirma; si Keycloak falla, revierte (503). La clave temporal sale de configuración y no se registra en logs. No se envían correos.
**Alternativas.** Autoregistro del usuario (no controla quién entra). Envío de código por correo (agrega SMTP y notificaciones, fuera de alcance). Saga completa con compensación explícita (excesiva para este caso).
**Consecuencias.** Riesgo residual: cuenta huérfana si falla el `COMMIT` tras crear la cuenta. Una clave temporal compartida es aceptable para el TPI; en producción conviene generarla aleatoria por usuario.

## ADR-013 — Tests sin Testcontainers
**Decisión.** Unitarios con Mockito, `@WebMvcTest` y `@DataJpaTest` con H2. Lo propio de PostgreSQL (índice parcial, `FOR UPDATE`) se verifica con Docker y Postman.
**Alternativas.** Testcontainers (no está en la cursada y exige Docker en los tests de JUnit).
**Consecuencias.** Menor fidelidad en tests automáticos para esas dos garantías; queda cubierta por la colección de integración.

## ADR-014 — Mapeo manual con records; Lombok solo en entidades
**Decisión.** DTO como `record`, mapeo manual en `mapper/`. Lombok únicamente en entidades, sin `@Data`.
**Alternativas.** MapStruct (no está en la cursada). Lombok en DTO (los `record` ya cubren ese caso, Apunte 5).
**Consecuencias.** Algo más de código explícito a cambio de claridad y cero magia.
