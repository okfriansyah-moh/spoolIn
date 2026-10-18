#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
service=""
follow=0
usage() { printf '%s\n' 'logs: use [service] [-f|--follow]; no other arguments are supported' >&2; }
for arg in "$@"; do
  case "$arg" in
    '') [[ $# == 1 ]] || { usage; exit 2; } ;;
    -f|--follow) [[ "$follow" == 0 ]] || { usage; exit 2; }; follow=1 ;;
    postgres|core|catalog|media|mcp|prometheus|alertmanager|grafana|core-migrator|catalog-migrator|media-migrator|catalog-seed)
      [[ -z "$service" ]] || { usage; exit 2; }; service="$arg" ;;
    *) usage; exit 2 ;;
  esac
done
# Keep complete application JSON lines; native dependency output retains its own format.
source "$root_dir/ops/bin/_compose_common.sh"
arguments=(logs --no-color --no-log-prefix --tail=200)
if [[ "$follow" == 1 ]]; then arguments+=(--follow); fi
if [[ -n "$service" ]]; then arguments+=("$service"); fi
compose_common_run "$root_dir" "${arguments[@]}"
