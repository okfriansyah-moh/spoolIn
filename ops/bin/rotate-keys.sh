#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
dry_run=1
from_alias=""
to_alias=""
purpose="DRAFT"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --execute) dry_run=0; shift ;;
    --from) from_alias="${2-}"; shift 2 ;;
    --to) to_alias="${2-}"; shift 2 ;;
    --purpose) purpose="${2-}"; shift 2 ;;
    *) printf 'unknown arg: %s\n' "$1" >&2; exit 2 ;;
  esac
done
if [[ -z "$from_alias" || -z "$to_alias" ]]; then
  printf '%s\n' "usage: rotate-keys.sh [--execute] --from <id> --to <id> [--purpose DRAFT|PII|EMAIL_LOOKUP]" >&2
  exit 2
fi
printf '%s\n' "rotate-keys: Core KeyRotationPlan from=$from_alias to=$to_alias purpose=$purpose dry_run=$dry_run"
printf '%s\n' "Invoke Core rotation entrypoint with mounted keys and database URL configured."
exit 0
