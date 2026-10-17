#!/usr/bin/env bash
set -euo pipefail
dry_run=1
task="${1:-vacuum}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --execute) dry_run=0; shift ;;
    vacuum|reindex) task="$1"; shift ;;
    *) shift ;;
  esac
done
: "${PGHOST:=localhost}"
: "${PGUSER:=spoolin_owner}"
case "$task" in
  vacuum) sql="VACUUM (VERBOSE, ANALYZE);" ;;
  reindex) sql="REINDEX DATABASE spoolin_catalog;" ;;
  *) printf 'unknown task: %s\n' "$task" >&2; exit 2 ;;
esac
cmd=(psql -h "$PGHOST" -U "$PGUSER" -d spoolin_catalog -c "$sql")
if [[ "$dry_run" -eq 1 ]]; then
  printf '%s\n' "maintenance dry-run: ${cmd[*]}"
  exit 0
fi
"${cmd[@]}"
