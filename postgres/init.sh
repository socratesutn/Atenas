#!/usr/bin/env bash
set -Eeuo pipefail

# PostgreSQL ejecuta este archivo una sola vez, cuando el volumen de datos
# todavía está vacío. Crea un esquema y un usuario por servicio para que cada
# microservicio quede aislado de los demás.
: "${POSTGRES_USER:?Falta POSTGRES_USER}"
: "${POSTGRES_DB:?Falta POSTGRES_DB}"
: "${CATEDRA_DB_PASSWORD:?Falta CATEDRA_DB_PASSWORD}"
: "${DOCUMENTACION_DB_PASSWORD:?Falta DOCUMENTACION_DB_PASSWORD}"
: "${SEGUIMIENTO_DB_PASSWORD:?Falta SEGUIMIENTO_DB_PASSWORD}"
: "${KEYCLOAK_DB_PASSWORD:?Falta KEYCLOAK_DB_PASSWORD}"

# Las claves se leen desde el entorno dentro de psql. Así no quedan escritas en
# el archivo ni aparecen en la línea de comandos del proceso.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 <<'SQL'
\getenv db POSTGRES_DB
\getenv catedra_pass CATEDRA_DB_PASSWORD
\getenv documentacion_pass DOCUMENTACION_DB_PASSWORD
\getenv seguimiento_pass SEGUIMIENTO_DB_PASSWORD
\getenv keycloak_pass KEYCLOAK_DB_PASSWORD

-- Crea el rol si no existe y después fija la clave. La creación se hace sin
-- contraseña para que \gexec no imprima el secreto en los logs; el ALTER ROLE
-- usa la variable de psql y no se muestra en la salida.
SELECT 'CREATE ROLE catedra_user LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'catedra_user');
\gexec
ALTER ROLE catedra_user LOGIN PASSWORD :'catedra_pass';

SELECT 'CREATE ROLE documentacion_user LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'documentacion_user');
\gexec
ALTER ROLE documentacion_user LOGIN PASSWORD :'documentacion_pass';

SELECT 'CREATE ROLE seguimiento_user LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'seguimiento_user');
\gexec
ALTER ROLE seguimiento_user LOGIN PASSWORD :'seguimiento_pass';

SELECT 'CREATE ROLE keycloak_user LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'keycloak_user');
\gexec
ALTER ROLE keycloak_user LOGIN PASSWORD :'keycloak_pass';

-- Cada usuario es dueño únicamente de su esquema.
CREATE SCHEMA IF NOT EXISTS catedra AUTHORIZATION catedra_user;
ALTER SCHEMA catedra OWNER TO catedra_user;
GRANT USAGE, CREATE ON SCHEMA catedra TO catedra_user;

CREATE SCHEMA IF NOT EXISTS documentacion AUTHORIZATION documentacion_user;
ALTER SCHEMA documentacion OWNER TO documentacion_user;
GRANT USAGE, CREATE ON SCHEMA documentacion TO documentacion_user;

CREATE SCHEMA IF NOT EXISTS seguimiento AUTHORIZATION seguimiento_user;
ALTER SCHEMA seguimiento OWNER TO seguimiento_user;
GRANT USAGE, CREATE ON SCHEMA seguimiento TO seguimiento_user;

CREATE SCHEMA IF NOT EXISTS keycloak AUTHORIZATION keycloak_user;
ALTER SCHEMA keycloak OWNER TO keycloak_user;
GRANT USAGE, CREATE ON SCHEMA keycloak TO keycloak_user;

-- El search_path se fija en la base concreta para que cada conexión use su
-- propio esquema sin depender de la configuración del cliente JDBC.
ALTER ROLE catedra_user IN DATABASE :"db" SET search_path = catedra;
ALTER ROLE documentacion_user IN DATABASE :"db" SET search_path = documentacion;
ALTER ROLE seguimiento_user IN DATABASE :"db" SET search_path = seguimiento;
ALTER ROLE keycloak_user IN DATABASE :"db" SET search_path = keycloak;

-- Evita que los usuarios accedan al esquema public por el permiso heredado de
-- PUBLIC. Cada uno queda limitado al esquema que le corresponde.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
SQL
