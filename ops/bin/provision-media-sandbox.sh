#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
[[ "$#" == 1 && ( "$1" == --install || "$1" == --check || "$1" == --validate-source ) ]] || exit 2
exec python3 "$root_dir/ops/bin/media-sandbox-policy.py" "$1"
