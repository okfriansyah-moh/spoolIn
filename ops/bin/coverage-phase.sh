#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$root_dir"
compose=(docker compose -f ops/unit-coverage/compose.yaml)
mkdir -p .local/unit-coverage
case "${1:-run}" in
  run)
    "${compose[@]}" build runner
    exec "${compose[@]}" run --rm --no-deps runner
    ;;
  report)
    test -f .local/unit-coverage/index.html || { echo 'Run make coverage first.' >&2; exit 1; }
    "${compose[@]}" --profile reports up -d --force-recreate reports
    echo "Coverage reports: http://localhost:${COVERAGE_HTTP_PORT:-8090}"
    ;;
  sonar-up)
    "${compose[@]}" --profile sonar up -d --wait sonar-db sonarqube
    echo "SonarQube: http://localhost:${SONAR_HTTP_PORT:-9000} (first login: admin/admin; change the password and create an administration token)"
    ;;
  sonar|sonar-analyze)
    : "${SONAR_TOKEN:?Set SONAR_TOKEN to a local SonarQube administration token}"
    "${compose[@]}" run --rm --no-deps -e SONAR_TOKEN runner node ops/unit-coverage/sonar.mjs prepare
    "${compose[@]}" --profile sonar build scanner
    failed=0
    for component in frontend core catalog media backend-support mcp; do
      "${compose[@]}" --profile sonar run --rm --no-deps scanner \
        "-Dproject.settings=.local/unit-coverage/sonar/$component.properties" || failed=1
    done
    if [[ "${1:-}" == sonar ]]; then
      "${compose[@]}" run --rm --no-deps -e SONAR_TOKEN runner node ops/unit-coverage/sonar.mjs verify || failed=1
    fi
    exit "$failed"
    ;;
  sonar-export-review|sonar-apply-review|sonar-verify)
    : "${SONAR_TOKEN:?Set SONAR_TOKEN to a local SonarQube administration token}"
    action="${1#sonar-}"
    "${compose[@]}" run --rm --no-deps -e SONAR_TOKEN -e GITHUB_ACTIONS -e GITHUB_TOKEN \
      -e GITHUB_REPOSITORY -e GITHUB_RUN_ID -e GITHUB_RUN_ATTEMPT -e GITHUB_ACTOR \
      -e SONAR_REVIEW_HEAD_SHA -e SONAR_REVIEW_PR_NUMBER -e SONAR_REVIEW_WAIT_SECONDS \
      runner node ops/unit-coverage/sonar.mjs "$action"
    ;;
  stop) exec "${compose[@]}" --profile sonar --profile reports down ;;
  *) echo 'Usage: coverage.sh {run|report|sonar-up|sonar|sonar-analyze|sonar-export-review|sonar-apply-review|sonar-verify|stop}' >&2; exit 2 ;;
esac
