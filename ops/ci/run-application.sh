#!/usr/bin/env bash
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true && "${RUNNER_ENVIRONMENT:-}" == github-hosted && "$(uname -s)" == Linux ]] || {
  printf '%s\n' 'Requires GitHub-hosted Linux; local and self-hosted execution refused' >&2
  exit 2
}
mkdir -p artifacts/runtime
{
  printf 'sha=%s\nrun=%s\nattempt=%s\nos=%s\nimage=%s\n' \
    "$(git rev-parse HEAD)" "$GITHUB_RUN_ID" "$GITHUB_RUN_ATTEMPT" "$RUNNER_OS" "${ImageVersion:-unavailable}"
  docker version --format '{{.Server.Version}}'
  docker compose version
  sha256sum data/seed/manifest.json
} > artifacts/runtime/environment.txt
export SPOOLIN_READINESS_REPORT="$PWD/artifacts/runtime/readiness-probes.json"
make start > artifacts/runtime/start.log 2>&1 || {
  code=$?
  make status > artifacts/runtime/readiness-failure.log 2>&1 || true
  make logs > artifacts/runtime/start-failure.log 2>&1 || true
  exit "$code"
}
make status > artifacts/runtime/readiness.log 2>&1
curl --fail --silent --show-error --connect-timeout 2 --max-time 10 --cacert "$SERVICE_CA_FILE" \
  "https://localhost:${CORE_HTTPS_PORT:-8443}/" -o artifacts/runtime/index.html
