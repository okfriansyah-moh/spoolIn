#!/usr/bin/env bash
# Source before local gates: Temurin 21.0.12.1+1 and Node 24.21.0 / npm 11.19.0.
set -euo pipefail
root_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

temurin="$HOME/.local/jdk/temurin-21.0.12.1+1/Contents/Home"
if [[ -x "$temurin/bin/java" ]]; then
  export JAVA_HOME="$temurin"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

node_bin="$HOME/.nvm/versions/node/v24.21.0/bin"
if [[ -x "$node_bin/node" ]]; then
  export PATH="$node_bin:$PATH"
elif [[ -f "$HOME/.nvm/nvm.sh" ]]; then
  export NVM_DIR="$HOME/.nvm"
  # shellcheck source=/dev/null
  source "$NVM_DIR/nvm.sh"
  nvm use 24.21.0 >/dev/null
fi
export PATH="$root_dir/node_modules/.bin:$PATH"
