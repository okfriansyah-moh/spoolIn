#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
manifest="$root_dir/data/seed/manifest.json"
validator="$root_dir/build-tools/validate-seed/validate_manifest.py"
test -f "$manifest" || { printf '%s\n' "missing $manifest" >&2; exit 2; }
python3 "$validator"
printf '%s\n' "seed: manifest validated; load via CatalogSeedLoader + Flyway catalog migrations when database is available"
printf '%s\n' "seed steps: 1) validate manifest 2) migrate catalog 3) bootstrap accounts 4) insert titles/genres from manifest"
exit 0
