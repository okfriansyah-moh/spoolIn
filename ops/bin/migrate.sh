#!/usr/bin/env bash
set -euo pipefail
[[ $# == 1 ]] || { printf '%s\n' 'migration: select exactly one owner' >&2; exit 2; }
case "${1-}" in core|catalog|media) owner="$1" ;; *) printf '%s\n' 'migration: select core, catalog or media' >&2; exit 2 ;; esac
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${SPOOLIN_INSTALLATION:?Set the installation identifier}"
[[ "$SPOOLIN_INSTALLATION" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || exit 2
: "${SPOOLIN_LOCK_DIR:?Set the protected installation lock directory}"
python3 -c 'import os,pathlib,stat,sys; p=pathlib.Path(sys.argv[1]); s=p.lstat(); sys.exit(0 if stat.S_ISDIR(s.st_mode) and stat.S_IMODE(s.st_mode)==0o700 and s.st_uid==os.getuid() else 2)' "$SPOOLIN_LOCK_DIR"
lock_dir="$SPOOLIN_LOCK_DIR/migration-$SPOOLIN_INSTALLATION.lock"
start=$SECONDS
until mkdir -m 700 -- "$lock_dir" 2>/dev/null; do
  if (( SECONDS - start >= 120 )); then printf '%s\n' 'migration: installation lock acquisition deadline exceeded' >&2; exit 4; fi
  sleep 1
done
compose=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml")
activated=false
cleanup() {
  code=$?
  if [[ "$activated" == true ]]; then
    "${compose[@]}" exec -T postgres /opt/spoolin/membership.sh revoke "$owner" || { (( code != 0 )) || code=4; }
  fi
  rmdir -- "$lock_dir" || { (( code != 0 )) || code=4; }
  exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
activated=true
"${compose[@]}" exec -T postgres /opt/spoolin/membership.sh grant "$owner" || exit 4
"${compose[@]}" --profile migration run --rm --no-deps "$owner-migrator" --migrate || exit 4
"${compose[@]}" exec -T postgres /opt/spoolin/membership.sh grants "$owner" || exit 4
