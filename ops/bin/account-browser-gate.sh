#!/usr/bin/env bash
# Starts Core accountBrowserFixture, runs Playwright account journeys, releases fixture.
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$root_dir"

# shellcheck source=/dev/null
source "$root_dir/ops/bin/toolchain-env.sh"
export ACCOUNT_BROWSER_ENGINE="${ACCOUNT_BROWSER_ENGINE:-chromium}"
export ACCOUNT_BROWSER_HEADED=0
export ACCOUNT_CERTUTIL="${ACCOUNT_CERTUTIL:-$(command -v certutil || echo /opt/certutil)}"

handoff="$root_dir/.ai/state/account-browser-public.json"
release="$root_dir/.ai/state/account-browser.release"
report="$root_dir/test-results/account/junit.xml"
mkdir -p "$(dirname "$report")"
rm -f "$report" "$release"

fixture_pid=""
cleanup() {
  if [[ -n "$fixture_pid" ]] && kill -0 "$fixture_pid" 2>/dev/null; then
    touch "$release" 2>/dev/null || true
    wait "$fixture_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

if [[ "${USE_ACCOUNT_BROWSER_CONTAINER:-}" == "1" ]]; then
  exec "$root_dir/ops/bin/run-account-browser-container.sh"
fi

./gradlew :services:core:accountBrowserFixture --console plain &
fixture_pid=$!

deadline=$((SECONDS + 900))
while (( SECONDS < deadline )); do
  if [[ -f "$handoff" ]]; then
    if node -e "
      const h=require('$handoff');
      const fs=require('fs');
      process.exit(h.origin && h.credentialsFile && fs.existsSync(h.credentialsFile) ? 0 : 1);
    "; then
      break
    fi
  fi
  if ! kill -0 "$fixture_pid" 2>/dev/null; then
    echo "accountBrowserFixture exited before handoff was ready" >&2
    wait "$fixture_pid" || true
    exit 1
  fi
  sleep 2
done

if ! [[ -f "$handoff" ]]; then
  echo "Timed out waiting for account browser handoff" >&2
  exit 1
fi

set +e
node "$root_dir/tests/browser/run-account.mjs"
account_status=$?
set -e

release_path="$(node -p "require('$handoff').releaseFile || '$release'")"
touch "$release_path"

wait "$fixture_pid" || true
fixture_pid=""

if [[ "$account_status" -ne 0 ]]; then
  exit "$account_status"
fi
if [[ ! -f "$report" ]]; then
  echo "Missing account JUnit report: $report" >&2
  exit 1
fi
