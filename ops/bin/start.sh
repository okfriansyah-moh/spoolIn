#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
compose=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml")
if ! command -v docker >/dev/null 2>&1; then
  printf '%s\n' "docker not found; aborting start wrapper" >&2
  exit 1
fi
if [[ ! -f "$root_dir/ops/compose.yaml" ]]; then
  printf '%s\n' "missing ops/compose.yaml" >&2
  exit 1
fi
printf '%s\n' "start: ${compose[*]} up -d (wrapper only; not executed in lane)"
exit 0
