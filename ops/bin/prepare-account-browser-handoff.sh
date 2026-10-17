#!/usr/bin/env bash
# Starts the Core account browser fixture and writes .ai/state/account-browser-public.json.
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$root_dir"
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "Set JAVA_HOME to a JDK 21 toolchain before running this script." >&2
  exit 1
fi
if ! command -v node >/dev/null; then
  echo "Node 24.21.0 is required (see package.json engines)." >&2
  exit 1
fi
mkdir -p .ai/state
echo "Starting account browser fixture (PostgreSQL + Core HTTPS). Stop with the fixture release file when done."
./gradlew :services:core:accountBrowserFixture --console plain
