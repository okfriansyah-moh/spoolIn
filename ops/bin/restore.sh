#!/usr/bin/env bash
set -euo pipefail
dry_run=1
input=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --execute) dry_run=0; shift ;;
    --input) input="${2-}"; shift 2 ;;
    *) printf 'unknown arg: %s\n' "$1" >&2; exit 2 ;;
  esac
done
if [[ -z "$input" ]]; then
  printf '%s\n' "usage: restore.sh [--execute] --input <dump>" >&2
  exit 2
fi
: "${PGHOST:=localhost}"
: "${PGUSER:=spoolin_owner}"
cmd=(pg_restore -h "$PGHOST" -U "$PGUSER" -d spoolin_catalog --clean --if-exists "$input")
if [[ "$dry_run" -eq 1 ]]; then
  printf '%s\n' "restore dry-run: ${cmd[*]}"
  exit 0
fi
"${cmd[@]}"
