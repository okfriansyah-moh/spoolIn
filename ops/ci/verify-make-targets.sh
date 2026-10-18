#!/usr/bin/env bash
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true && "${RUNNER_ENVIRONMENT:-}" == github-hosted ]] || exit 2
[[ "${SPOOLIN_INSTALLATION:-}" == ci-"$GITHUB_RUN_ID"-"$GITHUB_RUN_ATTEMPT" ]] || exit 2
mkdir -p artifacts/make
# Browser journeys must finish before this destructive lifecycle stage begins.
result=0
record() {
  local label="$1" expected="$2" code=0
  shift 2
  "$@" > "artifacts/make/$label.log" 2>&1 || code=$?
  printf '%s\t%s\t%s\n' "$label" "$code" "$expected" >> artifacts/make/results.tsv
  [[ "$code" == "$expected" ]] || result=1
}
printf 'command\texit\texpected\n' > artifacts/make/results.tsv
record status 0 make status
record logs 0 make logs
# The default seed target must report validation-only as incomplete, never success.
record seed-without-apply 2 make seed
record seed-apply 0 env SPOOLIN_SEED_APPLY=1 make seed
record restart 0 make restart
record post-restart-status 0 make status
record stop 0 make stop
# GNU Make reports a failed recipe as 2; the underlying health script returns 1.
record stopped-health-script 1 bash ops/bin/status.sh
record stopped-status 2 make status
record start 0 make start
record final-status 0 make status
# Execute the real repository-selected suites; missing selections retain their nonzero exits.
record test-unit 0 make test-unit
record test-integration 0 make test-integration
exit "$result"
