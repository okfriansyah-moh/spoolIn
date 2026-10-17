#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
service=""
follow=0
for arg in "$@"; do
  case "$arg" in
    -f | --follow) follow=1 ;;
    *)
      if [[ -z "$service" ]]; then
        service="$arg"
      fi
      ;;
  esac
done
# shellcheck source=_compose_common.sh
source "$root_dir/ops/bin/_compose_common.sh"
set +e
if [[ "$follow" -eq 1 ]]; then
  if [[ -n "$service" ]]; then
    compose_common_run "$root_dir" logs -f --tail=200 "$service"
  else
    compose_common_run "$root_dir" logs -f --tail=200
  fi
else
  if [[ -n "$service" ]]; then
    compose_common_run "$root_dir" logs --tail=200 "$service"
  else
    compose_common_run "$root_dir" logs --tail=200
  fi
fi
status=$?
set -e
exit "$status"
