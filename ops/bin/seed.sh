#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
manifest="$root_dir/data/seed/manifest.json"
validator="$root_dir/build-tools/validate-seed/validate_manifest.py"
apply=0
validate_only=0
for arg in "$@"; do
  case "$arg" in
    --apply) apply=1 ;;
    --validate-only) validate_only=1 ;;
    *) printf 'seed: unsupported argument: %s\n' "$arg" >&2; exit 2 ;;
  esac
done
if [[ "${SPOOLIN_SEED_APPLY:-}" == "1" ]]; then apply=1; fi
test -f "$manifest" || { printf '%s\n' "missing $manifest" >&2; exit 2; }
python3 "$validator" --structural-only "$manifest"
if [[ "$validate_only" -eq 1 ]]; then
  source "$root_dir/ops/bin/_compose_common.sh"
  compose_common_run "$root_dir" --profile seed run --rm --no-deps catalog-seed \
    --seed-manifest=/app/seed/manifest.json --validate-only
  exit 0
fi
if [[ "$apply" -ne 1 ]]; then
  printf '%s\n' "seed: structural preflight passed; effective-policy preparation/load skipped (pass --apply or set SPOOLIN_SEED_APPLY=1 to invoke CatalogSeedLoader)" >&2
  exit 2
fi
source "$root_dir/ops/bin/_compose_common.sh"
compose_common_run "$root_dir" --profile seed run --rm --no-deps catalog-seed
