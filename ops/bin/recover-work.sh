#!/usr/bin/env bash
# recover-work.sh — Restricted operator-authorized work recovery.
#
# Resets a single EXHAUSTED durable_work row to READY with an incremented generation,
# preserving original evidence and appending a RECOVERY operational_event.
#
# Usage:
#   ops/bin/recover-work.sh --service <core|catalog|media> \
#                           --work-id <uuid> \
#                           --operator-id <email|identity> \
#                           --reason <quoted reason>
#
# Required environment:
#   SPOOLIN_INSTALLATION  — installation identifier (e.g. spoolin-prod)
#   SPOOLIN_LOCK_DIR      — protected installation lock directory (mode 700)
#
# The script connects to the running Docker Compose database, applies the recovery
# operation via the Gradle application runner, and prints the result.
#
# Access must be gated by operator authentication before calling this script.
# This is not a browser feature.

set -euo pipefail

# ── argument parsing ────────────────────────────────────────────────────────────
service=""
work_id=""
operator_id=""
reason=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --service)   service="$2";    shift 2 ;;
        --work-id)   work_id="$2";    shift 2 ;;
        --operator-id) operator_id="$2"; shift 2 ;;
        --reason)    reason="$2";     shift 2 ;;
        *) printf 'recover-work: unknown argument: %s\n' "$1" >&2; exit 2 ;;
    esac
done

case "${service:-}" in
    core|catalog|media) ;;
    *) printf 'recover-work: --service must be core, catalog or media\n' >&2; exit 2 ;;
esac

[[ -n "${work_id:-}" ]]     || { printf 'recover-work: --work-id is required\n'     >&2; exit 2; }
[[ -n "${operator_id:-}" ]] || { printf 'recover-work: --operator-id is required\n' >&2; exit 2; }
[[ -n "${reason:-}" ]]      || { printf 'recover-work: --reason is required\n'       >&2; exit 2; }

# Validate UUID format (lowercase hyphenated)
if ! printf '%s' "$work_id" | grep -qE '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'; then
    printf 'recover-work: --work-id must be a lowercase hyphenated UUID\n' >&2; exit 2
fi

# ── environment checks ──────────────────────────────────────────────────────────
: "${SPOOLIN_INSTALLATION:?Set the installation identifier (SPOOLIN_INSTALLATION)}"
[[ "$SPOOLIN_INSTALLATION" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || {
    printf 'recover-work: SPOOLIN_INSTALLATION format invalid\n' >&2; exit 2
}
: "${SPOOLIN_LOCK_DIR:?Set the protected installation lock directory (SPOOLIN_LOCK_DIR)}"
python3 -c 'import os,pathlib,stat,sys; p=pathlib.Path(sys.argv[1]); s=p.lstat(); sys.exit(0 if stat.S_ISDIR(s.st_mode) and stat.S_IMODE(s.st_mode)==0o700 and s.st_uid==os.getuid() else 2)' "$SPOOLIN_LOCK_DIR" || {
    printf 'recover-work: SPOOLIN_LOCK_DIR must be a mode-700 directory owned by the current user\n' >&2; exit 2
}

# ── lock ────────────────────────────────────────────────────────────────────────
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
lock_dir="$SPOOLIN_LOCK_DIR/recover-work-$SPOOLIN_INSTALLATION.lock"
start=$SECONDS
until mkdir -m 700 -- "$lock_dir" 2>/dev/null; do
    if (( SECONDS - start >= 60 )); then
        printf 'recover-work: lock acquisition timed out\n' >&2; exit 4
    fi
    sleep 1
done
cleanup() {
    code=$?
    rmdir -- "$lock_dir" 2>/dev/null || true
    exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# ── execute ─────────────────────────────────────────────────────────────────────
compose=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml")

printf 'recover-work: recovering work %s in service %s as operator %s\n' \
    "$work_id" "$service" "$operator_id"

# Use the Gradle application runner to invoke WorkRecovery via a one-shot task.
# The SQL runs inside the service's connection pool with owner credentials.
JAVA_HOME="${JAVA_HOME:-/Users/mekari/.jdks/temurin-21/Contents/Home}" \
    SPOOLIN_RECOVER_SERVICE="$service" \
    SPOOLIN_RECOVER_WORK_ID="$work_id" \
    SPOOLIN_RECOVER_OPERATOR_ID="$operator_id" \
    SPOOLIN_RECOVER_REASON="$reason" \
    SPOOLIN_RECOVER_INSTALLATION="$SPOOLIN_INSTALLATION" \
    "$root_dir/gradlew" \
        ":services:$service:recoverWork" \
        --console plain \
        -q \
    || {
        printf 'recover-work: recovery task failed (exit %s)\n' "$?" >&2
        exit 1
    }

printf 'recover-work: done\n'
