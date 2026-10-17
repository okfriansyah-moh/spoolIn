#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
usage() {
  printf '%s\n' "usage: connector.sh issue|revoke|list --account <uuid> --client CLAUDE|CHATGPT" >&2
}
cmd="${1-}"
shift || { usage; exit 2; }
case "$cmd" in
  issue|revoke|list) ;;
  *) usage; exit 2 ;;
esac
printf '%s\n' "connector $cmd: configure Core gRPC and protected operator path (runtime proof pending)"
exit 0
