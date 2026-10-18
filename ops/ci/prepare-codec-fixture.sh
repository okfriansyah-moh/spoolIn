#!/usr/bin/env bash
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true && "${RUNNER_ENVIRONMENT:-}" == github-hosted && "${RUNNER_OS:-}" == Linux ]] || {
  printf '%s\n' 'Pinned codec preparation requires a verified GitHub-hosted Linux runner.' >&2
  exit 2
}
[[ "${ImageOS:-}" == ubuntu24 && -n "${RUNNER_TEMP:-}" && -n "${GITHUB_RUN_ID:-}" && -n "${GITHUB_RUN_ATTEMPT:-}" ]] || {
  printf '%s\n' 'This prerequisite targets the established Ubuntu 24.04 CI image and its owned temporary directory.' >&2
  exit 2
}
repository=$(git rev-parse --show-toplevel)
cd "$repository"
namespace="ci-$GITHUB_RUN_ID-$GITHUB_RUN_ATTEMPT"
image="spoolin-codec-fixture:$GITHUB_RUN_ID-$GITHUB_RUN_ATTEMPT"
staging=$(mktemp -d "$RUNNER_TEMP/spoolin-codec.XXXXXX")
container=''
cleanup() {
  [[ -z "$container" ]] || docker container rm "$container" >/dev/null
  docker image rm "$image" >/dev/null 2>&1 || true
  rm -rf "$staging"
}
trap cleanup EXIT
mkdir -p artifacts/source-checks
sudo apt-get update
sudo apt-get install -y --no-install-recommends bubblewrap util-linux \
  libglib2.0-0t64 libexif12 libexpat1 libjpeg-turbo8 libpng16-16t64 libwebp7 libwebpmux3 libwebpdemux2
# Build the production Dockerfile's pinned, checksum-verified codec and header helper.
docker build --file ops/containers/media.Dockerfile --target codec \
  --label "spoolin.test.namespace=$namespace" --tag "$image" .
container=$(docker create --label "spoolin.test.namespace=$namespace" --entrypoint /bin/true "$image")
mkdir "$staging/local"
docker cp "$container:/usr/local/." "$staging/local"
# Copy installed codec payloads without replacing runner-owned prefix links
# such as /usr/local/man with links inherited from the codec base image.
for directory in bin lib share; do
  [[ -d "$staging/local/$directory" ]] || continue
  sudo mkdir -p "/usr/local/$directory"
  sudo cp -a "$staging/local/$directory/." "/usr/local/$directory/"
done
sudo ldconfig
export LD_LIBRARY_PATH="/usr/local/lib/x86_64-linux-gnu:/usr/local/lib/aarch64-linux-gnu"
[[ "$(/usr/local/bin/vips --version)" == vips-8.18.7 ]] || {
  printf '%s\n' 'The required production libvips version is unavailable.' >&2
  exit 2
}
{
  printf 'assessed_sha=%s\nrunner_os=%s\nimage_os=%s\nimage_version=%s\n' "$(git rev-parse HEAD)" "$RUNNER_OS" "$ImageOS" "${ImageVersion:-unavailable}"
  docker image inspect "$image" --format 'codec_image_id={{.Id}}'
  sha256sum ops/containers/media.Dockerfile services/media/src/main/native/image-header.c /usr/local/bin/spoolin-image-header
  /usr/local/bin/vips --version
  /usr/bin/bwrap --version
  /usr/bin/prlimit --version
  ldd /usr/local/bin/spoolin-image-header
  printf '%s\n' 'Codec binaries come from the production build stage; host dependency versions below are recorded separately.'
  dpkg-query -W bubblewrap util-linux libglib2.0-0t64 libexif12 libexpat1 libjpeg-turbo8 libpng16-16t64 libwebp7 libwebpmux3 libwebpdemux2
} > artifacts/source-checks/codec-environment.txt
# Exercise the actual helper under production namespace/mount/resource restrictions.
# A denied namespace is a real setup failure; do not weaken AppArmor, seccomp or global user-namespace controls here.
python3 ops/ci/codec-diagnostics.py --output artifacts/source-checks/codec-host-before.json
probe_started="$(date -u +'%Y-%m-%d %H:%M:%S UTC')"
: > "$staging/dimensions"
if ! timeout 15s /usr/bin/bwrap --unshare-all --die-with-parent --new-session \
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 --proc /proc --dev /dev \
  --size 25161728 --tmpfs /tmp --ro-bind "$repository/docs/design-system/mockup/assets/letters.png" /input \
  --dir /output --bind "$staging/dimensions" /output/dimensions --remount-ro / --chdir /output \
  --clearenv --setenv PATH /usr/local/bin:/usr/bin --setenv LD_LIBRARY_PATH "$LD_LIBRARY_PATH" \
  --setenv TMPDIR /tmp --setenv VIPS_CONCURRENCY 1 \
  /usr/bin/prlimit --as=268435456 --cpu=5 --fsize=64 --nofile=64 -- \
  /usr/local/bin/spoolin-image-header /input /output/dimensions \
  > artifacts/source-checks/codec-sandbox-preflight.log 2>&1; then
  python3 ops/ci/codec-diagnostics.py --since "$probe_started" --probe-log artifacts/source-checks/codec-sandbox-preflight.log --output artifacts/source-checks/codec-host-after.json
  # The input and arguments above are fixed public fixture values; bound retained stderr and CI output.
  head -c 4096 artifacts/source-checks/codec-sandbox-preflight.log > "$staging/preflight-bounded"
  mv "$staging/preflight-bounded" artifacts/source-checks/codec-sandbox-preflight.log
  cat artifacts/source-checks/codec-sandbox-preflight.log >&2
  printf '%s\n' 'Production codec sandbox preflight failed. Inspect the runner namespace policy and dependencies; no unrestricted fallback was used.' >&2
  exit 2
fi
python3 ops/ci/codec-diagnostics.py --since "$probe_started" --probe-log artifacts/source-checks/codec-sandbox-preflight.log --output artifacts/source-checks/codec-host-after.json
head -c 4096 artifacts/source-checks/codec-sandbox-preflight.log > "$staging/preflight-bounded"
mv "$staging/preflight-bounded" artifacts/source-checks/codec-sandbox-preflight.log
[[ "$(cat "$staging/dimensions")" =~ ^[1-9][0-9]*\ [1-9][0-9]*\ [1-4]$ ]] || {
  printf '%s\n' 'Production header helper did not return valid bounded dimensions.' >&2
  exit 2
}
printf '%s\n' 'Pinned codec and namespace prerequisites are ready. Actual artworkIntegrationTest execution remains required.'
