#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
image="${ACCOUNT_BROWSER_IMAGE:-spoolin-account-browser:local}"
container="${ACCOUNT_BROWSER_CONTAINER:-spoolin-account-browser-gate}"

docker build -f "$root_dir/ops/containers/account-browser.Dockerfile" -t "$image" "$root_dir"
docker rm -f "$container" 2>/dev/null || true

handoff="$root_dir/.ai/state/account-browser-public.json"
public_dir="$root_dir/.ai/state/account-reader-public"
mkdir -p "$public_dir"
if [[ -f "$handoff" ]]; then
  node -e "
    const h=require('$handoff');
    require('fs').copyFileSync(h.ca, '$public_dir/ca.crt');
  "
fi

docker run -d --name "$container" --network host \
  -v "$root_dir:/work/project:ro" \
  -v "$public_dir:/work/public:ro" \
  "$image" sleep infinity

export ACCOUNT_BROWSER_CONTAINER="$container"
export USE_ACCOUNT_BROWSER_CONTAINER=0
"$root_dir/ops/bin/account-browser-gate.sh"
