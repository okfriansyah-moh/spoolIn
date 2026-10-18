#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
manifest="$root_dir/data/seed/manifest.json"
validator="$root_dir/build-tools/validate-seed/validate_manifest.py"
apply=0
for arg in "$@"; do
  if [[ "$arg" == "--apply" ]]; then apply=1; fi
done
if [[ "${SPOOLIN_SEED_APPLY:-}" == "1" ]]; then apply=1; fi
test -f "$manifest" || { printf '%s\n' "missing $manifest" >&2; exit 2; }
python3 "$validator"
if [[ "$apply" -ne 1 ]]; then
  printf '%s\n' "seed: manifest validated; load skipped (pass --apply or set SPOOLIN_SEED_APPLY=1 to invoke CatalogSeedLoader)" >&2
  exit 2
fi
gradle="$root_dir/gradlew"
if [[ ! -x "$gradle" ]]; then
  printf '%s\n' "seed --apply requires gradlew to invoke CatalogSeedLoader" >&2
  exit 1
fi
exec "$gradle" -p "$root_dir" :services:catalog:seedCli --args="--seed-manifest=$manifest"
