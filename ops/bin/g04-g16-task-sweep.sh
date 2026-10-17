#!/usr/bin/env bash
# Disabled on resource-constrained laptops — use CI or a remote host instead.
set -euo pipefail
echo "g04-g16-task-sweep: skipped (local browser/integration runners disabled to protect host RAM/CPU)." >&2
echo "Re-run per-task checks on CI or manually when safe: ops/bin/test-unit.sh --task Txxx" >&2
echo "Browser integrations: export SPOOLIN_TASK_BROWSER=1 then ops/bin/test-integration.sh --task Txxx" >&2
exit 0
