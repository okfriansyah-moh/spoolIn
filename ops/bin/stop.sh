#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
printf '%s\n' "stop: compose down for $SPOOLIN_INSTALLATION (not executed in lane)"
exit 0
