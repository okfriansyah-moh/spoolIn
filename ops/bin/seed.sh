#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
manifest="$root_dir/data/seed/manifest.json"
test -f "$manifest" || { printf '%s\n' "missing $manifest" >&2; exit 2; }
printf '%s\n' "seed: replay frozen manifest at $manifest (database replay pending runtime)"
exit 0
