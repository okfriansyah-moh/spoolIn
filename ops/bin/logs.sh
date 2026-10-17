#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
service="${1:-}"
compose=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml")
if [[ -n "$service" ]]; then
  printf '%s\n' "logs: ${compose[*]} logs -f --tail=200 $service (wrapper only)"
else
  printf '%s\n' "logs: ${compose[*]} logs -f --tail=200 (wrapper only)"
fi
exit 0
