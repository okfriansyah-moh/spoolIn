#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
example="$root_dir/data/seed/accounts.example.yaml"
test -f "$example" || { printf '%s\n' "missing $example" >&2; exit 2; }
printf '%s\n' "bootstrap-accounts: load prepared accounts from example (runtime pending)"
exit 0
