#!/usr/bin/env bash
set -euo pipefail
read_secret() {
  local path="$1" value size
  [[ -f "$path" && ! -L "$path" && "$(stat -c %a "$path")" == 600 ]] || return 2
  size="$(stat -c %s "$path")"
  (( size > 0 && size <= 4096 )) || return 2
  value="$(cat "$path" && printf '.')" || return 2
  value="${value%.}"
  printf '%s' "$value" | cmp -s - "$path" || return 2
  while [[ "$value" == *$'\n' || "$value" == *$'\r' ]]; do value="${value%?}"; done
  [[ "$value" =~ [^[:space:]] && "$value" != *$'\n'* && "$value" != *$'\r'* ]] || return 2
  printf '%s' "$value"
}
PGPASSWORD="$(read_secret /run/spoolin/postgres-password)"
export PGPASSWORD
{
  printf '\set ON_ERROR_STOP on\n'
  printf '\\set core_runtime_password '
  value="$(read_secret /run/spoolin/core-runtime-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set core_migrator_password '
  value="$(read_secret /run/spoolin/core-migrator-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set core_maintenance_password '
  value="$(read_secret /run/spoolin/core-maintenance-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set core_backup_password '
  value="$(read_secret /run/spoolin/core-backup-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_runtime_password '
  value="$(read_secret /run/spoolin/catalog-runtime-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_migrator_password '
  value="$(read_secret /run/spoolin/catalog-migrator-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_maintenance_password '
  value="$(read_secret /run/spoolin/catalog-maintenance-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_backup_password '
  value="$(read_secret /run/spoolin/catalog-backup-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_runtime_password '
  value="$(read_secret /run/spoolin/media-runtime-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_migrator_password '
  value="$(read_secret /run/spoolin/media-migrator-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_maintenance_password '
  value="$(read_secret /run/spoolin/media-maintenance-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_backup_password '
  value="$(read_secret /run/spoolin/media-backup-password)"
  escaped="${value//\\/\\\\}"
  escaped="${escaped//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  cat /opt/spoolin/bootstrap.sql
} | psql --username postgres --dbname postgres --no-psqlrc --quiet
unset PGPASSWORD value escaped
