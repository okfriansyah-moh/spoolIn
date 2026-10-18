#!/usr/bin/env bash
set -euo pipefail
umask 077
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
usage() {
  printf '%s\n' 'usage: connector.sh issue|revoke|list --account <uuid> --client CLAUDE|CHATGPT [--output-file <new-secret-file>] [--credential-id <uuid>] [--ttl-seconds <1..604800>]' >&2
}
[[ $# -gt 0 ]] || { usage; exit 2; }
cmd="$1"; shift
case "$cmd" in issue|revoke|list) ;; *) usage; exit 2 ;; esac
account='' client='' output='' credential='' ttl='604800'
seen=" "
while (( $# )); do
  [[ $# -ge 2 ]] || { usage; exit 2; }
  case "$seen" in *" $1 "*) usage; exit 2 ;; esac
  seen+="$1 "
  case "$1" in
    --account) account="$2" ;; --client) client="$2" ;; --output-file) output="$2" ;;
    --credential-id) credential="$2" ;; --ttl-seconds) ttl="$2" ;; *) usage; exit 2 ;;
  esac
  shift 2
done
[[ "$account" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ && "$client" =~ ^(CLAUDE|CHATGPT)$ ]] || exit 2
[[ "$ttl" =~ ^[0-9]{1,6}$ ]] && (( 10#$ttl >= 1 && 10#$ttl <= 604800 )) || exit 2
[[ "${SPOOLIN_INSTALLATION:-}" =~ ^[a-z][a-z0-9-]{2,63}$ ]] || exit 2
: "${SPOOLIN_LOCK_DIR:?Set installation-owned protected lock directory}"
python3 -c 'import os,pathlib,stat,sys; s=pathlib.Path(sys.argv[1]).lstat(); sys.exit(0 if stat.S_ISDIR(s.st_mode) and stat.S_IMODE(s.st_mode)==0o700 and s.st_uid==os.getuid() else 2)' "$SPOOLIN_LOCK_DIR"
mounts=() args=("--command=$cmd" "--account=$account" "--client=$client" "--ttl-seconds=$ttl")
if [[ "$cmd" == issue ]]; then
  [[ "$output" == /* && -z "$credential" && ! -e "$output" && ! -L "$output" ]] || exit 2
  directory="$(dirname -- "$output")" name="$(basename -- "$output")"
  [[ "$name" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$ ]] || exit 2
  python3 -c 'import os,pathlib,stat,sys; s=pathlib.Path(sys.argv[1]).lstat(); sys.exit(0 if stat.S_ISDIR(s.st_mode) and stat.S_IMODE(s.st_mode)==0o700 and s.st_uid==os.getuid() else 2)' "$directory"
  mounts=(--volume "$directory:/run/spoolin-connector-output:rw")
  args+=("--output=/run/spoolin-connector-output/$name")
elif [[ "$cmd" == revoke ]]; then
  [[ -z "$output" && "$credential" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]] || exit 2
  args+=("--credential-id=$credential")
else
  [[ -z "$output" && -z "$credential" ]] || exit 2
fi
lock="$SPOOLIN_LOCK_DIR/operator-$SPOOLIN_INSTALLATION.lock"
start=$SECONDS
until mkdir -m 700 "$lock" 2>/dev/null; do
  (( SECONDS - start < 60 )) || exit 4
  sleep 1
done
cleanup() { code=$?; rmdir -- "$lock" || { (( code != 0 )) || code=4; }; exit "$code"; }
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml" run --rm --no-deps -T \
  --user 0:0 --cap-add DAC_OVERRIDE --cap-add CHOWN --volume "$SPOOLIN_LOCK_DIR:/run/spoolin-operator-authority:ro" "${mounts[@]}" \
  --env "SPOOLIN_OPERATOR_UID=$(id -u)" --entrypoint java core \
  -Dloader.main=com.spoolin.core.features.connector.ConnectorOperatorCli \
  -cp /app/service.jar org.springframework.boot.loader.launch.PropertiesLauncher "${args[@]}"
