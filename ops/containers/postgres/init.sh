#!/usr/bin/env bash
set -euo pipefail
export PGPASSWORD="$(cat /run/spoolin/postgres-password)"
{
  printf '\set ON_ERROR_STOP on\n'
  printf '\\set core_runtime_password '
  value="$(cat /run/spoolin/core-runtime-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set core_migrator_password '
  value="$(cat /run/spoolin/core-migrator-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set core_maintenance_password '
  value="$(cat /run/spoolin/core-maintenance-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set core_backup_password '
  value="$(cat /run/spoolin/core-backup-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_runtime_password '
  value="$(cat /run/spoolin/catalog-runtime-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_migrator_password '
  value="$(cat /run/spoolin/catalog-migrator-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_maintenance_password '
  value="$(cat /run/spoolin/catalog-maintenance-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set catalog_backup_password '
  value="$(cat /run/spoolin/catalog-backup-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_runtime_password '
  value="$(cat /run/spoolin/media-runtime-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_migrator_password '
  value="$(cat /run/spoolin/media-migrator-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_maintenance_password '
  value="$(cat /run/spoolin/media-maintenance-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  printf '\\set media_backup_password '
  value="$(cat /run/spoolin/media-backup-password)"
  escaped="${value//\'/\'\'}"
  printf "'%s'\n" "$escaped"
  cat /opt/spoolin/bootstrap.sql
} | psql --username postgres --dbname postgres --no-psqlrc --quiet
unset PGPASSWORD value escaped
