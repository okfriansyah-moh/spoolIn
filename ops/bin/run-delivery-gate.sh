#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=/dev/null
source "$root_dir/ops/bin/toolchain-env.sh"
cd "$root_dir"
exec "$root_dir/.ai/bin/agent-workflow" gate --repo . --stage delivery --affected --base "${1:-7904722}" "${@:2}"
