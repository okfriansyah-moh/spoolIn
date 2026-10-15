#!/usr/bin/env bash
set -euo pipefail
case "${2-}" in core|catalog|media) owner="$2" ;; *) exit 2 ;; esac
export PGPASSWORD="$(cat /run/spoolin/postgres-password)"
case "${1-}" in
  grant) printf 'GRANT spoolin_%s_owner TO spoolin_%s_migrator;\n' "$owner" "$owner" | psql -X -q -v ON_ERROR_STOP=1 -U postgres -d "spoolin_$owner" ;;
  revoke) printf 'REVOKE spoolin_%s_owner FROM spoolin_%s_migrator;\n' "$owner" "$owner" | psql -X -q -v ON_ERROR_STOP=1 -U postgres -d "spoolin_$owner" ;;
  grants) psql -X -q -v ON_ERROR_STOP=1 -U postgres -d "spoolin_$owner" -f /opt/spoolin/grants.sql ;;
  *) exit 2 ;;
esac
