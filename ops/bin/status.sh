#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
compose=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml")
printf '%s\n' "status: ${compose[*]} ps (wrapper only; not executed in lane)"
exit 0
