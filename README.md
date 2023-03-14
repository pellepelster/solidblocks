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
SOLIDBLOCKS_SHELL_CHECKSUM="2c8fef536e0c4002b8e0db47a03d6c387d0adef160a8715fdb72d2b34f7ca8d1"

# self contained function for initial Solidblocks bootstrapping
function bootstrap_solidblocks() {
  local default_dir="$(cd "$(dirname "$0")" ; pwd -P)"
  local install_dir="${1:-${default_dir}/.solidblocks-shell}"
  local temp_file="$(mktemp)"

  curl -v -L "${SOLIDBLOCKS_BASE_URL:-https://github.com}/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_SHELL_VERSION}/solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" > "${temp_file}"
  file ${temp_file}
  echo "${SOLIDBLOCKS_SHELL_CHECKSUM}  ${temp_file}" | sha256sum -c
  mkdir -p "${install_dir}" || true
  (
      cd "${install_dir}"
      unzip -o -j "${temp_file}" -d "${install_dir}"
      rm -f "${temp_file}"
  )
}

# makes sure all needed shell functions functions are available and all bootstrapped software is on the $PATH
function ensure_environment() {

  if [[ ! -d "${DIR}/.solidblocks-shell" ]]; then
    echo "environment is not bootstrapped, please run ./do bootstrap first"
    exit 1
  fi

  # included needed shell functions
  source "${DIR}/.solidblocks-shell/log.sh"
  source "${DIR}/.solidblocks-shell/utils.sh"
  source "${DIR}/.solidblocks-shell/pass.sh"
  source "${DIR}/.solidblocks-shell/colors.sh"
  source "${DIR}/.solidblocks-shell/software.sh"

  # ensure $PATH contains all software downloaded via the `software_ensure_*` functions
  software_set_export_path
}

# bootstrap Solidblocks, and all other software needed using the software installer helpers from https://pellepelster.github.io/solidblocks/shell/software/
function task_bootstrap() {
  bootstrap_solidblocks
  ensure_environment
  software_ensure_terraform
}

# run the downloaded terraform version, ensure_environment ensures the downloaded versions takes precedence over any system binaries
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
