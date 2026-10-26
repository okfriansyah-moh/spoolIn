#!/usr/bin/env bash
# Local operator transport; the server credential stays inside its existing container.
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
[[ "${SPOOLIN_INSTALLATION:-}" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || {
  printf '%s\n' 'Set the owned SPOOLIN_INSTALLATION before identity migration.' >&2
  exit 2
}
exec docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml" \
  exec -T -e PGCONNECT_TIMEOUT="${PGCONNECT_TIMEOUT:-10}" \
  -e PGOPTIONS="${PGOPTIONS:--c statement_timeout=30000 -c lock_timeout=5000}" postgres bash -c \
  'export PGPASSWORD="$(cat /run/spoolin/postgres-password)"; exec psql "$@"' identity-psql "$@"
