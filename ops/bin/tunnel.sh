#!/usr/bin/env bash
set -euo pipefail
usage() { printf '%s\n' 'usage: tunnel.sh start|stop|status' >&2; }
[[ $# == 1 ]] || { usage; exit 2; }
case "$1" in start|stop|status) ;; *) usage; exit 2 ;; esac
printf '%s\n' 'tunnel: no verified registered Secure MCP Tunnel runner is configured; install the approved pinned runner, registered tunnel identity and protected runtime credential before lifecycle execution' >&2
exit 78
