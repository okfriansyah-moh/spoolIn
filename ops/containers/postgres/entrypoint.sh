#!/usr/bin/env bash
set -euo pipefail
install -d -o postgres -g postgres -m 700 /run/spoolin
install -o postgres -g postgres -m 600 /run/secrets/postgres-key /run/spoolin/server.key
install -o postgres -g postgres -m 644 /run/secrets/postgres-cert /run/spoolin/server.crt
for path in /run/secrets/*-password; do
  install -o postgres -g postgres -m 600 "$path" "/run/spoolin/$(basename "$path")"
done
export POSTGRES_PASSWORD_FILE=/run/spoolin/postgres-password
exec /usr/local/bin/docker-entrypoint.sh "$@"
