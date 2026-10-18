#!/usr/bin/env bash
set -euo pipefail
[[ $# == 0 ]] || { printf '%s\n' 'restart: no arguments supported' >&2; exit 2; }
"$(dirname -- "${BASH_SOURCE[0]}")/stop.sh"
"$(dirname -- "${BASH_SOURCE[0]}")/start.sh"
