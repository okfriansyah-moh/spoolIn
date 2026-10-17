#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
service="${1:-}"
# shellcheck source=_compose_common.sh
source "$root_dir/ops/bin/_compose_common.sh"
set +e
if [[ -n "$service" ]]; then
  compose_common_run "$root_dir" logs -f --tail=200 "$service"
else
  compose_common_run "$root_dir" logs -f --tail=200
fi
status=$?
set -e
exit "$status"
