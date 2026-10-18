#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
if [[ -n ${TASK:-} ]]; then
  set -- --task "$TASK" "$@"
fi
exec python3 "$root_dir/build-tools/verification-tests/task_selector.py" integration "$@"
