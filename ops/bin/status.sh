#!/usr/bin/env bash
set -euo pipefail
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
printf '%s\n' "status: inspect compose services for $SPOOLIN_INSTALLATION (not executed in lane)"
exit 0
