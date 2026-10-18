#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=_compose_common.sh
source "$root_dir/ops/bin/_compose_common.sh"
if [[ -z "${SPOOLIN_ACCOUNT_INITIALIZATION_FILE:-}" ]]; then
  printf '%s\n' 'bootstrap-accounts: set protected SPOOLIN_ACCOUNT_INITIALIZATION_FILE inside the Core runtime config mount' >&2
  exit 2
fi
[[ "$SPOOLIN_ACCOUNT_INITIALIZATION_FILE" == /run/spoolin-config/* ]] || exit 2
compose_common_run "$root_dir" run --rm --no-deps core \
  "--initialize-accounts=$SPOOLIN_ACCOUNT_INITIALIZATION_FILE"
