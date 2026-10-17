#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
printf '%s\n' "start: use docker compose --project-name $SPOOLIN_INSTALLATION -f $root_dir/ops/compose.yaml up -d (not executed in lane)"
exit 0
