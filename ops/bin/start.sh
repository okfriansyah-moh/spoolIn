#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=_compose_common.sh
source "$root_dir/ops/bin/_compose_common.sh"
set +e
compose_common_run "$root_dir" up -d
status=$?
set -e
exit "$status"
