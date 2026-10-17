# shellcheck shell=bash
# Shared Compose invocation for lifecycle scripts (T037).
compose_common_run() {
  local root_dir action
  root_dir="$1"
  shift
  action=("$@")
  : "${SPOOLIN_INSTALLATION:?Set SPOOLIN_INSTALLATION}"
  local -a compose
  compose=(docker compose --project-name "$SPOOLIN_INSTALLATION" -f "$root_dir/ops/compose.yaml")
  if [[ "${SPOOLIN_SKIP_COMPOSE:-}" == "1" ]]; then
    printf '%s\n' "compose skipped (SPOOLIN_SKIP_COMPOSE=1): ${compose[*]} ${action[*]}" >&2
    return 78
  fi
  if ! command -v docker >/dev/null 2>&1; then
    printf '%s\n' "docker not found; compose action not run" >&2
    return 78
  fi
  if [[ ! -f "$root_dir/ops/compose.yaml" ]]; then
    printf '%s\n' "missing ops/compose.yaml" >&2
    return 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    printf '%s\n' "docker compose plugin unavailable" >&2
    return 78
  fi
  "${compose[@]}" "${action[@]}"
}
