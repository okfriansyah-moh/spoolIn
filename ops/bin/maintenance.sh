#!/usr/bin/env bash
set -euo pipefail
umask 077
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$root_dir/ops/bin/_compose_common.sh"
service="" schedule="" mode=inspect
while (( $# )); do
  case "$1" in
    --service|--schedule)
      (( $# >= 2 )) || exit 2
      case "$1" in
        --service) [[ -z "$service" ]] || exit 2; service="$2" ;;
        --schedule) [[ -z "$schedule" ]] || exit 2; schedule="$2" ;;
      esac
      shift 2 ;;
    --execute) [[ "$mode" == inspect ]] || exit 2; mode=execute; shift ;;
    *) printf '%s\n' 'maintenance: use --service core|catalog|media --schedule hourly|daily [--execute]' >&2; exit 2 ;;
  esac
done
[[ "$service" =~ ^(core|catalog|media)$ && "$schedule" =~ ^(hourly|daily)$ ]] || exit 2
compose_common_run "$root_dir" run --rm --no-deps -T --entrypoint java "$service" \
  -Dloader.main=com.spoolin.support.maintenance.RetentionMaintenanceCli \
  -cp /app/service.jar org.springframework.boot.loader.launch.PropertiesLauncher \
  "--owner=$service" "--schedule=$schedule" "--mode=$mode"
