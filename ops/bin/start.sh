#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
bin="$root_dir/ops/bin"
# shellcheck source=_compose_common.sh
source "$root_dir/ops/bin/_compose_common.sh"

run_migrations() {
  if [[ -z "${SPOOLIN_INSTALLATION:-}" || -z "${SPOOLIN_LOCK_DIR:-}" ]]; then
    printf '%s\n' "start: skipping migrations (set SPOOLIN_INSTALLATION and SPOOLIN_LOCK_DIR)" >&2
    return 0
  fi
  for owner in core catalog media; do
    "$bin/migrate.sh" "$owner" || exit 4
  done
}

run_seed() {
  SPOOLIN_SEED_APPLY=1 "$bin/seed.sh" --apply || exit 4
}

run_migrations
run_seed
set +e
compose_common_run "$root_dir" up -d
status=$?
set -e
exit "$status"
