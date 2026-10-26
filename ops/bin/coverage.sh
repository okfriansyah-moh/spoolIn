#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$root_dir"
case "${1:-run}" in
  run|sonar|sonar-up)
    exec .ai/bin/agent-workflow run --repo . --timeout 7200 --log ".ai/reports/docker-${1:-run}.log" -- bash ops/bin/coverage-phase.sh "${1:-run}"
    ;;
  *) exec bash ops/bin/coverage-phase.sh "${1:-run}" ;;
esac
