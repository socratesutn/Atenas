# AGENTS.md — ms-catedra

Leé primero el `AGENTS.md` de la raíz y `docs/07-estandar-microservicio.md`.

## Responsabilidad
Estructura académica (cursos lectivos, comisiones, comisiones lectivas), docentes, estudiantes, equipos e integrantes. Alta/baja de cuentas en Keycloak. **Fuente de verdad de personas y pertenencia**: los demás servicios le preguntan por `/internal/**`.

## Datos
Puerto dev **8082** · esquema **`catedra`** (usuario `catedra_user`) · modelo en `docs/05-modelo-de-datos.md`.

## Qué implementa
- Endpoints públicos **C01–C22** y contratos internos **I01–I05** (`docs/06-contratos-api.md`).
- Reglas: RN01–RN03, RN05–RN11 (`docs/03-reglas-de-negocio.md`).
- Funcionalidades: F02, F03, F04, F05.

## Particularidades
- **State** en `Equipo` (`ABIERTO` / `ASEGURADO`): paquete `estado/` (ver `docs/10`). Las reglas de *quién* puede (docente/representante) van en el `service`; el estado solo decide si la operación es válida (409).
- **Alta de usuario** (RN03): insertar → `flush` → `KeycloakAdminClient` (token con Client Credentials del cliente `atenas-catedra-admin`) → crear usuario con `CLAVE_TEMPORAL` y cambio obligatorio → asignar rol → guardar `sub` → commit. Si Keycloak falla: revertir y 503. La llamada a Keycloak ocurre dentro del flujo de alta; no loguear la clave.
- **Ingreso a equipo**: bloquear la fila del equipo con `SELECT … FOR UPDATE` (cupo). El índice único parcial `integrante(estudiante_id) WHERE fecha_baja IS NULL` va en `src/main/resources/data.sql` (`CREATE UNIQUE INDEX IF NOT EXISTS`, con `spring.jpa.defer-datasource-initialization=true` y `spring.sql.init.mode=always`).
- Estudiante: legajo numérico único; el mail se deriva `{legajo}@sistemas.frc.utn.edu.ar`; `usuario_github` en minúsculas.
- Los endpoints `/internal/**` los consumen `ms-documentacion` y `ms-seguimiento`; respetar la forma de `docs/06`.
- Clientes salientes: solo `KeycloakAdminClient`.

## Tests propios
Unitarios de `service` para cada RN de arriba, tests de cada transición de `estado/`, `@WebMvcTest` por controlador (401/403 por rol), `@DataJpaTest` para consultas propias. En H2 el `data.sql` se desactiva (`spring.sql.init.mode=never`).

## Este servicio NO
Guarda entregas, asistencia ni datos de GitHub; ni llama a `ms-documentacion` o `ms-seguimiento`.
