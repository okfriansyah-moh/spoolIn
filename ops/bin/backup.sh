#!/usr/bin/env bash
set -euo pipefail
dry_run=1
output=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --execute) dry_run=0; shift ;;
    --output) output="${2-}"; shift 2 ;;
    *) printf 'unknown arg: %s\n' "$1" >&2; exit 2 ;;
  esac
done
: "${PGHOST:=localhost}"
: "${PGUSER:=spoolin_owner}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output="${output:-/var/backups/spoolin-$timestamp.dump}"
cmd=(pg_dump -Fc -h "$PGHOST" -U "$PGUSER" -d spoolin_catalog -f "$output")
if [[ "$dry_run" -eq 1 ]]; then
  printf '%s\n' "backup dry-run: ${cmd[*]}"
  exit 0
fi
"${cmd[@]}"
