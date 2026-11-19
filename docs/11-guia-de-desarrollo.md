# 11 — Guía de desarrollo y puesta en marcha

> Para entender el porqué: Apunte 3 (Maven), 21-Docker y 22 (Docker, Compose, healthchecks, redes, `.env`).

## Requisitos
JDK 21 · Maven 3.9 · Docker Desktop con Compose v2 · Git · Postman · un token de GitHub y una API key de Google Drive.

## Puertos
| Componente | Dev (IDE) | Docker |
|---|---|---|
| `api-gateway` | 8080 | **8080 publicado** |
| Keycloak | 8081 | **8081 publicado** → 8080 interno |
| `ms-catedra` | 8082 | 8080 interno |
| `ms-documentacion` | 8083 | 8080 interno |
| `ms-seguimiento` | 8084 | 8080 interno |
| PostgreSQL | 5432 (con `docker-compose.dev.yml`) | 5432 interno |

## Variables de entorno (`.env`, no versionado; `.env.example` sin valores reales)
| Variable | Para qué |
|---|---|
| `POSTGRES_PASSWORD` | Superusuario de PostgreSQL |
| `CATEDRA_DB_PASSWORD`, `DOCUMENTACION_DB_PASSWORD`, `SEGUIMIENTO_DB_PASSWORD`, `KEYCLOAK_DB_PASSWORD` | Usuario propio de cada esquema |
| `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | Administrador de Keycloak |
| `KC_CATEDRA_ADMIN_SECRET` | Secreto del cliente `atenas-catedra-admin` |
| `KC_SEGUIMIENTO_JOB_SECRET` | Secreto del cliente `atenas-seguimiento-job` |
| `JEFE_PASSWORD_INICIAL` | Clave inicial del jefe cargado en el realm |
| `CLAVE_TEMPORAL` | Clave temporal asignada a docentes y alumnos nuevos (Keycloak obliga a cambiarla) |
| `GITHUB_TOKEN` | Token de servicio para la API de GitHub (5000 pedidos/hora) |
| `DRIVE_API_KEY` | API key de Google Drive |

Cada servicio las mapea a propiedades `atenas.*` en su `application-*.yml`.

## Perfiles
| Aspecto | `dev` (IDE) | `docker` (Compose) |
|---|---|---|
| Activación | `SPRING_PROFILES_ACTIVE=dev` | La fija Compose |
| URLs entre servicios | `http://localhost:<puerto>` | `http://ms-catedra:8080`, etc. |
| Base de datos | `localhost:5432` | `postgres:5432` |
| `jwk-set-uri` | `http://localhost:8081/realms/atenas/protocol/openid-connect/certs` | `http://keycloak:8080/realms/atenas/protocol/openid-connect/certs` |
| `issuer-uri` | `http://localhost:8081/realms/atenas` | `http://localhost:8081/realms/atenas` (el `iss` es el mismo para clientes externos y contenedores) |

Keycloak usa `KC_HOSTNAME=http://localhost:8081` y `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`.

## Arranque completo (entorno de entrega)
1. `cp .env.example .env` y completar.
2. `cd atenas-commons && mvn clean install`
3. `mvn clean package` en `api-gateway`, `ms-catedra`, `ms-documentacion`, `ms-seguimiento`.
4. `docker compose up --build`. PostgreSQL ejecuta `postgres/init.sh` (esquemas y usuarios, con las claves del `.env`); Keycloak importa `keycloak/realm.json`; cuando ambos están *healthy* arrancan los servicios y Hibernate crea las tablas; `ms-catedra` ejecuta `data.sql` (índice único parcial, con `CREATE UNIQUE INDEX IF NOT EXISTS`).
5. Verificar: `docker compose ps` (solo Gateway y Keycloak con puertos), obtener un JWT en Postman e invocar `http://localhost:8080/api/...`.
6. Base limpia: `docker compose down -v`.

## Desarrollo diario (servicio en el IDE)
```
docker compose -f docker-compose.yml -f docker-compose.dev.yml up postgres keycloak
set -a; source .env; set +a
cd ms-catedra && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Si cambia `atenas-commons`: `mvn clean install` ahí y volver a compilar el servicio.

## Postman
`postman/atenas.postman_collection.json` y `postman/atenas.postman_environment.json` (sin claves). Carpetas numeradas por orden de ejecución: `01 Estructura y usuarios`, `02 Equipos`, `03 Calendario y repositorio`, `04 Proceso principal`, `05 Consultas del alumno`, `06 Reglas de dominio`, `07 Negativos`, `08 Fallas controladas`. Autenticación:
**Autenticación (convención para TODAS las requests).**
- Cada request declara su propia autorización: **Bearer Token con `{{token-<usuario>}}`**. Ninguna request hereda el token de la carpeta ni del jefe. Las únicas sin token son las que prueban el 401 y la documentación OpenAPI (*No Auth*).
- Variables del environment (vacías en el archivo versionado): `token-jefe`, `token-docente-a`, `token-docente-b`, `token-representante`, `token-integrante`, `token-alumno-b`.
- Carpeta `00 Tokens`: un request por usuario configurado con OAuth 2.0 Authorization Code (cliente `atenas-postman`, callback `https://oauth.pstmn.io/v1/callback`) para obtener el token con un clic ("Get New Access Token") y pegarlo en su variable. El flujo es interactivo (navegador): no se puede automatizar dentro de una corrida.
- **Orden:** primero `token-jefe`; después se ejecuta la carpeta 01 (crea docentes y estudiantes); recién entonces se obtienen los tokens de docentes y alumnos. Cada usuario nuevo inicia sesión con `CLAVE_TEMPORAL` y Keycloak le pide cambiarla (por eso se usa Authorization Code y no un flujo con usuario y clave directos).
- Los access tokens vencen a las **4 horas**. En el realm de desarrollo se fija esa vida (`accessTokenLifespan`) para poder preparar y exponer sin 401 a mitad; si vence, se vuelve a obtener el token.
Cada request lleva `X-Correlation-Id: {{$guid}}` y tests que verifican código y campos y guardan IDs en variables. Parte de una base vacía (`down -v`).
**Regla:** cada historia agrega al Postman las requests de lo que implementa.

## Git
- `main` siempre compila y pasa los tests. Nadie pushea directo salvo la inicialización del día 1.
- Ramas por **conjunto de funcionalidades de un solo microservicio**: `feature/<servicio>-<tema>` (ej. `feature/catedra-equipos`). Se integran a `main` por Pull Request.
- Commits en español con el tipo convencional: `feat(catedra): crea espacios de equipo`, `test(documentacion): cubre RN29`, `fix`, `docs`, `refactor`, `chore`.
- Archivos compartidos (`docker-compose*.yml`, `keycloak/realm.json`, `postgres/init.sh`, `.env.example`, `atenas-commons/`): los cambia la persona A o con su aviso. Un cambio en `atenas-commons` se avisa al equipo.

## Definición de terminado (cada historia)
- [ ] Hace solo lo que pide la historia y respeta `docs/`.
- [ ] Tests nuevos (unitarios de reglas, controlador, estados/clientes según corresponda) y `mvn clean test` en verde.
- [ ] El servicio levanta y `/actuator/health` responde `UP`.
- [ ] OpenAPI refleja el endpoint (`@Operation`, respuestas de error).
- [ ] Requests agregadas a la colección Postman.
- [ ] Sin secretos ni tokens en código o logs.
- [ ] PR con lista de archivos tocados y cómo probarlo a mano.

## Plan hasta la defensa (defensa: vie 20/11)
| Día | Meta |
|---|---|
| **Lun 16** | A: inicialización completa (prompts P-01 a P-07). B: `GitHubClient` y cálculo de actividad con tests. C y D: entidades/repositorios de `ms-documentacion` y spike de Drive. Fin del día: `docker compose up` con los 3 servicios, Gateway y Keycloak; `/actuator/health` UP; 401 en el Gateway |
| **Mar 17** | A: `ms-catedra` (estructura, usuarios con Keycloak, equipos) y sus endpoints `/internal`. B: repositorio, calendario y asistencia contra interfaces `client` (Mockito hasta que existan I01–I07). C: actividades y entrega. D: consultas y corrección |
| **Mié 18** | Integración real entre servicios; proceso central de punta a punta; dashboard; tarea nocturna; Postman de las carpetas 01–05 |
| **Jue 19** | Congelar funcionalidad al mediodía. Postman completo (06–08), fallas controladas, README, ensayo de la defensa con `12` |
| **Vie 20** | Defensa |

Línea de corte (se avisa a la cátedra): F14–F16.

## Reparto propuesto (45 / 35 / 15 / 15)
| Persona | Responsable de |
|---|---|
| **A — 45 %** | Toda la inicialización (estructura, `atenas-commons`, esqueletos y `/actuator/health` de los tres servicios, Gateway, Compose, Keycloak, PostgreSQL) y **`ms-catedra`** completo |
| **B — 35 %** | **`ms-seguimiento`**. Empieza por lo que no depende de nadie: `GitHubClient`, cálculo de actividad y snapshots |
| **C — 15 %** | `ms-documentacion` (parte 1): hitos (D01, D02), entrega (D03), `DriveClient`, `I06` |
| **D — 15 %** | `ms-documentacion` (parte 2): consultas (D04, D05), apertura y corrección (D06, D07), `I07`; consolida la colección Postman |

(Quién es quién lo definen entre ustedes, considerando los parciales.)

## Qué probar temprano (spike del día 1)
- **Keycloak:** que el `iss` sea `http://localhost:8081/realms/atenas` desde fuera y desde los contenedores; que la cuenta `atenas-catedra-admin` pueda crear y deshabilitar usuarios; el `healthcheck` (la imagen no trae `curl`: usar una comprobación TCP o el endpoint de salud del puerto de gestión).
- **Swagger por el Gateway:** que `/api/<servicio>/swagger-ui.html` cargue sus recursos; si no, publicar solo `/api/<servicio>/v3/api-docs`.
- **Drive** (C): que una API key lea metadatos y `modifiedTime` de un PDF y de un Google Doc públicos.
- **GitHub** (B): que el token liste commits de una rama con `since`/`until` y devuelva `parents` para detectar merges.
