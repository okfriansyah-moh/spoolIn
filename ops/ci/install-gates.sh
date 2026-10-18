#!/usr/bin/env bash
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true && "${RUNNER_ENVIRONMENT:-}" == github-hosted && "$(uname -m)" == x86_64 ]] || exit 2
archive_dir="$RUNNER_TEMP/spoolin-gate-downloads"
bin_dir="$RUNNER_TEMP/spoolin-gate-bin"
mkdir -p "$archive_dir" "$bin_dir"
curl --fail --location --proto '=https' --tlsv1.2 --retry 2 \
  https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz \
  --output "$archive_dir/gitleaks.tar.gz"
printf '%s  %s\n' 551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb "$archive_dir/gitleaks.tar.gz" | sha256sum --check
tar -xzf "$archive_dir/gitleaks.tar.gz" -C "$bin_dir" gitleaks
curl --fail --location --proto '=https' --tlsv1.2 --retry 2 \
  https://github.com/google/osv-scanner/releases/download/v2.7.0/osv-scanner_linux_amd64 \
  --output "$bin_dir/osv-scanner"
printf '%s  %s\n' 160c6253e2922e9e0a6a9771c5c4c40a4b3c6090b34a3cdd5e80c96cc2e4218b "$bin_dir/osv-scanner" | sha256sum --check
chmod 755 "$bin_dir/gitleaks" "$bin_dir/osv-scanner"
python3 -m venv "$RUNNER_TEMP/spoolin-semgrep"
"$RUNNER_TEMP/spoolin-semgrep/bin/pip" install --disable-pip-version-check 'semgrep==1.180.0'
printf '%s\n' "$bin_dir" "$RUNNER_TEMP/spoolin-semgrep/bin" >> "$GITHUB_PATH"
mkdir -p artifacts/provenance
{
  "$bin_dir/gitleaks" version
  "$bin_dir/osv-scanner" --version
  "$RUNNER_TEMP/spoolin-semgrep/bin/semgrep" --version
  node --version
  npm --version
  java -version 2>&1
} > artifacts/provenance/tool-versions.txt
