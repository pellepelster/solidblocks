[![solidblocks](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml/badge.svg)](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml)

# Solidblocks

Solidblocks is a library of reusable components for infrastructure operation, automation and developer experience. It consists of several components, each covering a different infrastructure aspect, full documentation is available at https://pellepelster.github.io/solidblocks/.

## Components

### Solidblocks Shell

Reusable shell functions for infrastructure automation and developer experience. See below for a full example how the shell functions can easily be used in any shell script:

```shell
#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

SOLIDBLOCKS_SHELL_VERSION="snapshot"
SOLIDBLOCKS_SHELL_CHECKSUM="141811205a715840a8a3d0e55105eaf754f917cf56df6a1e1767e0984dd26ae6"

# self contained function for initial Solidblocks bootstrapping
function bootstrap_solidblocks() {
  local default_dir="$(cd "$(dirname "$0")" ; pwd -P)"
  local install_dir="${1:-${default_dir}/.solidblocks-shell}"

  local temp_file="$(mktemp)"

  mkdir -p "${install_dir}"
  curl -L "${SOLIDBLOCKS_BASE_URL:-https://github.com}/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_SHELL_VERSION}/solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" > "${temp_file}"
  echo "${SOLIDBLOCKS_SHELL_CHECKSUM}  ${temp_file}" | sha256sum -c
  cd "${install_dir}"
  unzip -o -j "${temp_file}" -d "${install_dir}"
  rm -f "${temp_file}"
}

# makes sure all lib functions are available
# and all bootstrapped software is on the $PATH
function ensure_environment() {

  if [[ ! -d "${DIR}/.solidblocks-shell" ]]; then
    echo "environment is not bootstrapped, please run ./do bootstrap first"
    exit 1
  fi

  source "${DIR}/.solidblocks-shell/log.sh"
  source "${DIR}/.solidblocks-shell/utils.sh"
  source "${DIR}/.solidblocks-shell/pass.sh"
  source "${DIR}/.solidblocks-shell/colors.sh"
  source "${DIR}/.solidblocks-shell/software.sh"

  software_set_export_path
}

# bootstrapping of Solidblocks and all
# needed software for the project
function task_bootstrap() {
  bootstrap_solidblocks
  ensure_environment

  software_ensure_terraform
}

# run the downloaded terraform version, ensure_environment ensures
# the downloaded versions takes precedence over any system binaries
function task_terraform {
  terraform -version
}

#
function task_usage {
  cat <<EOF
Usage: $0

  bootstrap               initialize the development environment

  ${FORMAT_BOLD}deployment${FORMAT_RESET}

    terraform             run terraform
EOF
  exit 1
}

ARG=${1:-}
shift || true

# if we see the boostrap command assume Solidshell is not yet initialized and skip environment setup
case "${ARG}" in
  bootstrap) ;;
  *) ensure_environment ;;
esac

case ${ARG} in
  bootstrap) task_bootstrap "$@" ;;
  terraform) task_terraform "$@" ;;
  *) task_usage ;;
esac
```
