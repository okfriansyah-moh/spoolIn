#!/usr/bin/env bash
set -euo pipefail
# Install only the opted-in distro bwrap policy on the disposable hosted runner.
[[ "${GITHUB_ACTIONS:-}" == true && "${RUNNER_ENVIRONMENT:-}" == github-hosted && "${RUNNER_OS:-}" == Linux && "${ImageOS:-}" == ubuntu24 ]] || {
  printf '%s\n' 'Codec profile preparation requires GitHub-hosted Ubuntu 24.04.' >&2; exit 2;
}
[[ "${GITHUB_RUN_ID:-}" =~ ^[0-9]+$ && "${GITHUB_RUN_ATTEMPT:-}" =~ ^[0-9]+$ && -d "${RUNNER_TEMP:-}" && -O "$RUNNER_TEMP" && ! -L "$RUNNER_TEMP" && "$EUID" -ne 0 ]] || {
  printf '%s\n' 'Codec profile preparation requires an owned runner directory and non-root caller.' >&2; exit 2;
}
fail() {
  printf 'Codec profile prerequisite: %s\n' "$1" >&2
  exit 2
}
# Fixed OS data and fixed interpreter paths; no downloaded scripts are executed.
. /etc/os-release
[[ "$ID" == ubuntu && "$VERSION_ID" == 24.04 && -r /usr/share/keyrings/ubuntu-archive-keyring.gpg ]] || fail "Ubuntu 24.04 archive keyring is required."
[[ -f /usr/bin/bwrap && ! -L /usr/bin/bwrap && ! -u /usr/bin/bwrap && ! -g /usr/bin/bwrap ]] || fail "Regular non-setuid distro bwrap is required."
[[ "$(stat -c '%u:%a' /usr/bin/bwrap)" == 0:755 ]] || fail "Unexpected bwrap ownership or mode."
[[ -z "$(/usr/sbin/getcap /usr/bin/bwrap)" ]] || fail "File capabilities on bwrap are forbidden."
[[ "$(cat /sys/module/apparmor/parameters/enabled)" == Y ]] || fail "AppArmor must already be enabled."
restriction=/proc/sys/kernel/apparmor_restrict_unprivileged_userns
[[ "$(cat "$restriction")" == 1 ]] || fail "Restricted unprivileged user namespaces must remain enabled."
repository=$(git rev-parse --show-toplevel)
cd "$repository"
validator="$repository/ops/ci/codec-profile-package.py"
profile=/etc/apparmor.d/bwrap-userns-restrict
# Never replace an existing policy or activate site-specific overrides implicitly.
for existing in "$profile" /etc/apparmor.d/bwrap /etc/apparmor.d/bubblewrap /etc/apparmor.d/usr.bin.bwrap \
  /etc/apparmor.d/local/bwrap-userns-restrict /etc/apparmor.d/local/unpriv_bwrap \
  /etc/apparmor.d/disable/bwrap-userns-restrict /etc/apparmor.d/force-complain/bwrap-userns-restrict; do
  [[ ! -e "$existing" && ! -L "$existing" ]] || {
    printf '%s\n' 'Existing bubblewrap policy or override requires explicit inspection.' >&2; exit 2;
  }
done
staging=$(mktemp -d "$RUNNER_TEMP/spoolin-codec-profile.XXXXXX")
trap 'sudo rm -rf -- "$staging"' EXIT
# These directories contain public package data only; _apt retains its download sandbox.
record_controls() {
  awk '/^(CapEff|CapBnd|NoNewPrivs|Seccomp):/ { print $1 " " $2 }' "/proc/$$/status"
  for control in apparmor_restrict_unprivileged_userns apparmor_restrict_unprivileged_unconfined unprivileged_userns_clone; do
    if [[ -r "/proc/sys/kernel/$control" ]]; then
      printf '%s %s\n' "$control" "$(cat "/proc/sys/kernel/$control")"
    fi
  done
}
record_controls > "$staging/controls-before"
rg -q '^CapEff: 0000000000000000$' "$staging/controls-before" || fail "Caller must have no effective capabilities."
chmod 755 "$staging"
mkdir -p "$staging/lists" "$staging/download"
sudo install -d -o _apt -g root -m 700 "$staging/lists/partial"
printf '%s\n' 'deb [signed-by=/usr/share/keyrings/ubuntu-archive-keyring.gpg] https://archive.ubuntu.com/ubuntu noble-updates main' > "$staging/sources.list"
apt_options=(-o "Dir::Etc::sourcelist=$staging/sources.list" -o 'Dir::Etc::sourceparts=-'
  -o "Dir::State::lists=$staging/lists" -o 'APT::Get::AllowUnauthenticated=false'
  -o 'Acquire::AllowInsecureRepositories=false' -o 'Acquire::AllowDowngradeToInsecureRepositories=false'
  -o 'Acquire::Check-Valid-Until=true' -o 'Acquire::https::Timeout=30' -o 'Acquire::Retries=2')
# Fresh isolated indexes bind this package to the signed Ubuntu noble-updates source.
sudo timeout 180s apt-get "${apt_options[@]}" -o 'APT::Update::Error-Mode=any' update
version=4.0.1really4.0.1-0ubuntu0.24.04.9
apt-cache "${apt_options[@]}" show "apparmor-profiles=$version" > "$staging/package-metadata"
python3 "$validator" metadata "$staging/package-metadata"
(
  cd "$staging/download"
  timeout 90s apt-get "${apt_options[@]}" download "apparmor-profiles=$version"
)
package="$staging/download/apparmor-profiles_${version}_all.deb"
python3 "$validator" package "$package"
# Unpack as the non-root caller; package maintainer scripts are never run.
dpkg-deb --extract "$package" "$staging/payload"
payload="$staging/payload/usr/share/apparmor/extra-profiles/bwrap-userns-restrict"
python3 "$validator" profile "$payload"
sudo cat /sys/kernel/security/apparmor/profiles > "$staging/profiles-before"
if rg -q '^(bwrap|unpriv_bwrap|/usr/bin/bwrap)( |//|$)' "$staging/profiles-before"; then
  printf '%s\n' 'A loaded bubblewrap policy requires explicit inspection before replacement.' >&2; exit 2
fi
sudo install -o root -g root -m 644 "$payload" "$profile"
# Add only these profiles, with default enforce mode; no global reload or complain flag.
sudo /sbin/apparmor_parser --add "$profile"
sudo cat /sys/kernel/security/apparmor/profiles > "$staging/profiles-after"
python3 "$validator" loaded "$staging/profiles-after"
[[ "$(cat "$restriction")" == 1 && -z "$(/usr/sbin/getcap /usr/bin/bwrap)" ]] || fail "Namespace restriction or bwrap capabilities changed."
record_controls > "$staging/controls-after"
cmp --silent "$staging/controls-before" "$staging/controls-after" || {
  printf '%s\n' 'Caller security controls changed during profile preparation.' >&2; exit 2;
}
mkdir -p artifacts/source-checks
python3 "$validator" report artifacts/source-checks/codec-profile-prerequisite.json \
  --sha "$(git rev-parse HEAD)" --run-id "$GITHUB_RUN_ID" --attempt "$GITHUB_RUN_ATTEMPT" --image-version "${ImageVersion:-unavailable}" --controls-file "$staging/controls-after"
printf '%s\n' 'Distro restrictive profile loaded. Unchanged host probe and production container probe remain required.'
