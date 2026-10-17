#!/usr/bin/env bash
set -euo pipefail
: "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
service="${1-}"
printf '%s\n' "logs: tail $service for $SPOOLIN_INSTALLATION (not executed in lane)"
exit 0
