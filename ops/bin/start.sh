#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
bin="$root_dir/ops/bin"
# shellcheck source=_compose_common.sh
source "$bin/_compose_common.sh"
: "${SPOOLIN_INSTALLATION:?Set installation identifier}"
: "${SPOOLIN_LOCK_DIR:?Set protected migration lock directory}"
[[ "$SPOOLIN_INSTALLATION" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || exit 2
# Building is a separate bounded stage; initialization starts only after PostgreSQL is ready.
timeout 900 docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml" build || exit 3
compose_common_run "$root_dir" run --rm --no-deps --entrypoint /bin/sh core -c \
  'test -r /run/spoolin-config/core.json && test ! -e /run/spoolin-config/restore-pending-validation' || {
  printf '%s\n' 'start: protected runtime configuration or restored installation validation is incomplete' >&2
  exit 4
}
compose_common_run "$root_dir" up -d --wait --wait-timeout 120 postgres || exit 5
for owner in core catalog media; do
  "$bin/migrate.sh" "$owner" || exit 4
done
"$bin/bootstrap-accounts.sh" || exit 4
SPOOLIN_SEED_APPLY=1 "$bin/seed.sh" --apply || exit 4
compose_common_run "$root_dir" up -d core catalog media || exit 5
compose_common_run "$root_dir" up -d mcp || exit 5
compose_common_run "$root_dir" up -d --wait --wait-timeout 120 prometheus alertmanager grafana || exit 5
umask 077
probe="$(mktemp "${TMPDIR:-/tmp}/spoolin-readiness.XXXXXXXX")"
trap 'rm -f -- "$probe"' EXIT
start=$SECONDS
until "$bin/status.sh" >"$probe" 2>&1; do
  if (( SECONDS - start >= 300 )); then
    printf '%s\n' 'start: readiness deadline exceeded; last bounded readiness probe follows' >&2
    head -c 16384 "$probe" >&2
    exit 5
  fi
  sleep 2
done
cat -- "$probe"
printf '%s\n' 'start: Core, Catalog, Media and MCP ready; database migrations and catalog seed applied'
printf '%s\n' 'start: external chat clients and monitoring are unverified; inspect acceptance obligations'
