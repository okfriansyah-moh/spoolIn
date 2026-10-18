#!/usr/bin/env bash
set -euo pipefail
umask 077
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
service="" work_id="" reason="" generation="" epoch="" mode=inspect
while (( $# )); do
  case "$1" in
    --service|--work-id|--reason|--expected-generation|--expected-epoch)
      (( $# >= 2 )) || exit 2
      case "$1" in
        --service) service="$2" ;; --work-id) work_id="$2" ;; --reason) reason="$2" ;;
        --expected-generation) generation="$2" ;; --expected-epoch) epoch="$2" ;;
      esac
      shift 2 ;;
    --execute) mode=execute; shift ;;
    --inspect) mode=inspect; shift ;;
    *) printf '%s\n' 'recover-work: unsupported argument; use --inspect or --execute with the inspected generation/epoch and reason' >&2; exit 2 ;;
  esac
done
[[ "$service" =~ ^(core|catalog|media)$ && "$work_id" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]] || exit 2
[[ "${SPOOLIN_INSTALLATION:-}" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || exit 2
: "${SPOOLIN_LOCK_DIR:?Set installation-owned protected lock directory}"
python3 -c 'import os,pathlib,stat,sys; p=pathlib.Path(sys.argv[1]); s=p.lstat(); sys.exit(0 if stat.S_ISDIR(s.st_mode) and stat.S_IMODE(s.st_mode)==0o700 and s.st_uid==os.getuid() else 2)' "$SPOOLIN_LOCK_DIR"
if [[ "$mode" == execute ]]; then
  [[ "$generation" =~ ^[0-9]{1,9}$ && "$epoch" =~ ^[0-9]{1,19}$ && -n "$reason" ]] || exit 2
fi
lock="$SPOOLIN_LOCK_DIR/operator-$SPOOLIN_INSTALLATION.lock"
start=$SECONDS
until mkdir -m 700 "$lock" 2>/dev/null; do
  (( SECONDS - start < 60 )) || exit 4
  sleep 1
done
temporary="$(mktemp -d "$SPOOLIN_LOCK_DIR/recovery.XXXXXXXX")"
cleanup() {
  code=$?
  rm -f -- "$temporary/reason"
  rmdir -- "$temporary" "$lock" || { (( code != 0 )) || code=4; }
  exit "$code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
printf '%s' "$reason" > "$temporary/reason"
chmod 600 "$temporary/reason"
command=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml" run --rm --no-deps -T
  --user 0:0 --cap-add DAC_OVERRIDE --volume "$SPOOLIN_LOCK_DIR:/run/spoolin-operator-authority:ro"
  --volume "$temporary/reason:/run/spoolin-operator-reason:ro" --env "SPOOLIN_OPERATOR_UID=$(id -u)"
  --entrypoint java "$service" -Dloader.main=com.spoolin.support.work.WorkRecoveryCli
  -cp /app/service.jar org.springframework.boot.loader.launch.PropertiesLauncher
  "--owner=$service" "--work-id=$work_id" "--mode=$mode")
if [[ "$mode" == execute ]]; then
  command+=("--expected-generation=$generation" "--expected-epoch=$epoch")
fi
"${command[@]}"
