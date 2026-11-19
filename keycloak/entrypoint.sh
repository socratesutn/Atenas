#!/usr/bin/env bash
set -Eeuo pipefail

# Keycloak no reemplaza los placeholders del realm al importar en esta versión.
# Por eso preparamos una copia temporal del realm con los secretos del entorno
# y arrancamos Keycloak con esa copia. Así los valores sensibles no quedan
# escritos en el repositorio ni en docker-compose.yml.
for variable in KC_CATEDRA_ADMIN_SECRET KC_SEGUIMIENTO_JOB_SECRET JEFE_PASSWORD_INICIAL; do
  if [ -z "${!variable:-}" ]; then
    echo "Falta la variable de entorno $variable" >&2
    exit 1
  fi
done

plantilla=/opt/keycloak/realm-template.json
destino=/opt/keycloak/data/import/realm.json
mkdir -p "$(dirname "$destino")"

contenido=$(<"$plantilla")
contenido=${contenido//'${KC_CATEDRA_ADMIN_SECRET}'/${KC_CATEDRA_ADMIN_SECRET}}
contenido=${contenido//'${KC_SEGUIMIENTO_JOB_SECRET}'/${KC_SEGUIMIENTO_JOB_SECRET}}
contenido=${contenido//'${JEFE_PASSWORD_INICIAL}'/${JEFE_PASSWORD_INICIAL}}

printf '%s\n' "$contenido" > "$destino"

# "$@" conserva el comando start-dev --import-realm definido en Compose.
exec /opt/keycloak/bin/kc.sh "$@"
