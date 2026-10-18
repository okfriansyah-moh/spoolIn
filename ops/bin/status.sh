#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$root_dir/ops/bin/_compose_common.sh"
compose_common_run "$root_dir" ps || exit 1
: "${SERVICE_CA_FILE:?Set trusted service CA}"
: "${SPOOLIN_OPERATOR_CERT_FILE:?Set operator client certificate}"
: "${SPOOLIN_OPERATOR_KEY_FILE:?Set operator client key}"
for port in "${CORE_MANAGEMENT_PORT:-18443}" "${CATALOG_MANAGEMENT_PORT:-18444}" "${MEDIA_MANAGEMENT_PORT:-18445}"; do
  body="$(curl --silent --show-error --fail --max-time 5 --cacert "$SERVICE_CA_FILE" \
    --cert "$SPOOLIN_OPERATOR_CERT_FILE" --key "$SPOOLIN_OPERATOR_KEY_FILE" \
    "https://localhost:$port/actuator/health/readiness")" || exit 1
  python3 -c 'import json,sys; sys.exit(0 if json.load(sys.stdin).get("status")=="UP" else 1)' <<<"$body" || exit 1
done
mcp_health="$(compose_common_run "$root_dir" ps --format json mcp)" || exit 1
python3 -c 'import json,sys; item=json.load(sys.stdin); sys.exit(0 if item.get("Health")=="healthy" else 1)' <<<"$mcp_health" || exit 1
printf '%s\n' 'status: selected application services ready; external client acceptance unverified'
