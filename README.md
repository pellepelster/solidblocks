[![solidblocks](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml/badge.svg)](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml)

# Solidblocks

Solidblocks is a library of reusable components for infrastructure  automation, documentation is available here  https://pellepelster.github.io/solidblocks/

## Shell

Reusable shell functions for infrastructure automation ([documentation](https://pellepelster.github.io/solidblocks/shell/))

All functions are tested on the following distributions

* Amazon Linux 2
* Debian 10
* Debian 11
* Ubuntu 20.04
* Ubuntu 22.04

### Usage example

#### Installation
```shell
function bootstrap_solidblocks() {
  local default_dir="$(cd "$(dirname "$0")" ; pwd -P)"
  local install_dir="${1:-${default_dir}/.solidblocks-shell}"

  SOLIDBLOCKS_SHELL_VERSION="v0.0.68"
  SOLIDBLOCKS_SHELL_CHECKSUM="1a7bb1d03b35e4cb94d825ec542d6f51c2c3cc1a3c387b0dea61eb4be32760a7"

  local temp_file="$(mktemp)"

  mkdir -p "${install_dir}"
  curl -L "https://github.com/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_SHELL_VERSION}/solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" > "${temp_file}"
  echo "${SOLIDBLOCKS_SHELL_CHECKSUM}  ${temp_file}" | sha256sum -c
  cd "${install_dir}"
  unzip -o -j "${temp_file}" -d "${install_dir}"
  rm -f "${temp_file}"
}

bootstrap_solidblocks
```

#### Create Script
**example.sh**
```shell
#!/usr/bin/env bash

source "solidblocks-shell/software.sh"

software_ensure_terraform
software_set_export_path

terraform version
```

#### Run Script

```
$ ./example.sh

Usage: terraform [-version] [-help] <command> [args]

The available commands for execution are listed below.
The most common, useful commands are shown first, followed by
less common or more advanced commands. If you're just getting
started with Terraform, stick with the common commands. For the
other commands, please read the help and docs before usage.

Common commands:
    apply              Builds or changes infrastructure
    console            Interactive console for Terraform interpolations
    [...]
    version            Prints the Terraform version
    workspace          Workspace management

All other commands:
    0.12upgrade        Rewrites pre-0.12 module source code for v0.12
    [...]
    state              Advanced state management

```
