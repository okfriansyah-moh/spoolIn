#!/usr/bin/env bash
set -euo pipefail
[[ $# == 0 ]] || { printf '%s\n' 'stop: no arguments supported' >&2; exit 2; }
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=_compose_common.sh
source "$root_dir/ops/bin/_compose_common.sh"
set +e
compose_common_run "$root_dir" down --timeout 30
status=$?
set -e
exit "$status"
