#!/usr/bin/env bash
set -euo pipefail
umask 077
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
usage() {
  printf '%s\n' 'usage: rotate-keys.sh [--dry-run|--execute] --from ID --to ID [--purpose DRAFT|PII|EMAIL_LOOKUP] [--register-target] [--max-batches 1..10000]' >&2
}
refuse() { printf '%s\n' "rotation: $1" >&2; exit 2; }
mode=dry-run from='' to='' purpose=DRAFT batches=100 register=0 seen=' '
while (( $# )); do
  case "$seen" in *" $1 "*) usage; exit 2 ;; esac
  seen+="$1 "
  case "$1" in
    --execute|--dry-run)
      [[ "$seen" != *' --execute '* || "$seen" != *' --dry-run '* ]] || { usage; exit 2; }
      mode="${1#--}"; shift ;;
    --register-target) register=1; shift ;;
    --from|--to|--purpose|--max-batches)
      (( $# >= 2 )) && [[ "$2" != --* ]] || { usage; exit 2; }
      case "$1" in --from) from="$2" ;; --to) to="$2" ;; --purpose) purpose="$2" ;; --max-batches) batches="$2" ;; esac
      shift 2 ;;
    *) usage; exit 2 ;;
  esac
done
[[ "$from" =~ ^[A-Za-z0-9_-]{1,64}$ && "$to" =~ ^[A-Za-z0-9_-]{1,64}$ && "$from" != "$to" ]] || { usage; exit 2; }
[[ "$purpose" =~ ^(DRAFT|PII|EMAIL_LOOKUP)$ && "$batches" =~ ^[1-9][0-9]{0,4}$ ]] && (( batches <= 10000 )) || { usage; exit 2; }
[[ "${SPOOLIN_INSTALLATION:-}" =~ ^[a-z][a-z0-9-]{2,63}$ && -n "${SPOOLIN_LOCK_DIR:-}" ]] || refuse 'protected installation and lock directory required'
python3 -c 'import os,pathlib,stat,sys; s=pathlib.Path(sys.argv[1]).lstat(); sys.exit(0 if stat.S_ISDIR(s.st_mode) and stat.S_IMODE(s.st_mode)==0o700 and s.st_uid==os.getuid() else 2)' "$SPOOLIN_LOCK_DIR" \
  2>/dev/null || refuse 'operator lock directory must be owned, nonsymlinked and mode 0700'
source "$root_dir/ops/bin/_compose_common.sh"
lock="$SPOOLIN_LOCK_DIR/operator-$SPOOLIN_INSTALLATION.lock"
start=$SECONDS
until mkdir -m 700 "$lock" 2>/dev/null; do
  (( SECONDS - start < 60 )) || exit 4
  sleep 1
done
stopped=0
cleanup() {
  code=$?
  if (( stopped )); then
    compose_common_run "$root_dir" up -d --no-deps core || { (( code != 0 )) || code=4; }
  fi
  rmdir -- "$lock" || { (( code != 0 )) || code=4; }
  exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
args=(--from "$from" --to "$to" --purpose "$purpose" --max-batches "$batches")
if [[ "$mode" == dry-run ]]; then
  args+=(--dry-run)
else
  # All application Core writers stop before registration/activation; the operator command remains the sole writer.
  running="$(compose_common_run "$root_dir" ps --status running --quiet core)"
  if [[ -n "$running" ]]; then
    stopped=1
    compose_common_run "$root_dir" stop core
  fi
fi
(( register == 0 )) || args+=(--register-target)
compose_common_run "$root_dir" run --rm --no-deps -T \
  --user 0:0 --cap-add DAC_OVERRIDE --volume "$SPOOLIN_LOCK_DIR:/run/spoolin-operator-authority:ro" \
  --env "SPOOLIN_OPERATOR_UID=$(id -u)" --entrypoint java core \
  -Dloader.main=com.spoolin.core.operations.crypto.KeyRotationCliKt \
  -cp /app/service.jar org.springframework.boot.loader.launch.PropertiesLauncher "${args[@]}"
