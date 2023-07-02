[![solidblocks](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml/badge.svg)](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml)

# Solidblocks

Solidblocks is a library of reusable components for infrastructure operations, automation and developer experience. It consists of several components, each covering a different infrastructure aspect, full documentation is available at https://pellepelster.github.io/solidblocks/.

## Components

### [Solidblocks Hetzner Nuke](https://pellepelster.github.io/solidblocks/hetzner/nuke/) 

Hetzner nuke is a tool to delete all resources in a Hetzner account, similar to aws-nuke.

#### Usage Example

```shell
docker run -e HCLOUD_TOKEN="${HCLOUD_TOKEN}" ghcr.io/pellepelster/solidblocks-hetzner-nuke:v0.1.15 nuke
```

### [Solidblocks Terraform](https://pellepelster.github.io/solidblocks/terraform/) 

Helpers to bootstrap terraform storage backends.

#### Usage Example

```shell
docker run \
    -e AWS_REGION="eu-central-1" \
    -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
    -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
    ghcr.io/pellepelster/solidblocks-terraform:snapshot \
    backend s3 "${bucket-name}"
```

### [Solidblocks Cloud Init](https://pellepelster.github.io/solidblocks/cloud-init/)

Based on [Shell](https://pellepelster.github.io/solidblocks/shell/) reusable shell functions for typical [Cloud-Init](https://cloudinit.readthedocs.io/en/latest/) user-data usage scenarios

#### Usage Example

```shell
#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"

SOLIDBLOCKS_VERSION="v0.1.15"
SOLIDBLOCKS_CLOUD_CHECKSUM="405f82aefb78c8099f98e2913699d323398f7238965ef8ce60c96556139ddd03"

function bootstrap_package_update {
  apt-get update
}

function bootstrap_package_update_system() {
    apt-get \
        -o Dpkg::Options::="--force-confnew" \
        --force-yes \
        -fuy \
        dist-upgrade
}

function bootstrap_package_check_and_install {
	local package=${1}

	echo -n "checking if package '${package}' is installed..."

	if [[ $(dpkg-query -W -f='${Status}' "${package}" 2>/dev/null | grep -c "ok installed") -eq 0 ]];
	then
		echo "not found, installing now"
		while ! DEBIAN_FRONTEND="noninteractive" apt-get install --no-install-recommends -qq -y "${package}"; do
    		echo "installing failed retrying in 30 seconds"
    		sleep 30
    		apt-get update
		done
	else
		echo "ok"
	fi
}

function bootstrap_solidblocks() {
  bootstrap_package_update
  bootstrap_package_check_and_install "unzip"

  groupadd solidblocks
  useradd solidblocks -g solidblocks

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{templates,lib}

  chmod 770 ${SOLIDBLOCKS_DIR}
  chown solidblocks:solidblocks ${SOLIDBLOCKS_DIR}

  chmod -R 770 ${SOLIDBLOCKS_DIR}
  chown -R solidblocks:solidblocks ${SOLIDBLOCKS_DIR}

  local temp_file="$(mktemp)"

  curl -v -L "${SOLIDBLOCKS_BASE_URL:-https://github.com}/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.zip" > "${temp_file}"
  echo "${SOLIDBLOCKS_CLOUD_CHECKSUM}  ${temp_file}" | sha256sum -c

  (
    cd "${SOLIDBLOCKS_DIR}" || exit 1
    unzip "${temp_file}"
    rm -rf "${temp_file}"
  )

  source "${SOLIDBLOCKS_DIR}/lib/storage.sh"
}

bootstrap_solidblocks

storage_mount "/dev/sdb1" "/data1"
```

### [Solidblocks RDS PostgreSQL](https://pellepelster.github.io/solidblocks/rds/)

A containerized PostgreSQL database with all batteries included backup solution powered by pgBackRest

#### Usage Example

```shell
mkdir postgres_{data,backup} && sudo chown 10000:10000 postgres_{data,backup}

docker run \
    --name instance1 \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_backup:/storage/backup" \
    -v "$(pwd)/postgres_data:/storage/data" \
    ghcr.io/pellepelster/solidblocks-rds-postgresql:v0.1.15
```

### [Solidblocks Hetzner RDS PostgreSQL](https://pellepelster.github.io/solidblocks/hetzner/rds-postgresql/)

Based on the RDS PostgreSQL docker image this Terraform module provides a ready to use PostgreSQL server that is backed up to a S3 compatible object store.


#### Usage Example

```terraform
data "aws_s3_bucket" "backup" {
  bucket = "test-rds-postgresql-backup"
}

data "hcloud_volume" "data" {
  name = "rds-postgresql-data"
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "rds-postgresql"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

module "rds-postgresql" {
  source  = "pellepelster/solidblocks-rds-postgresql/hcloud"
  version = "0.1.15"

  name     = "rds-postgresql"
  location = var.hetzner_location

  ssh_keys = [hcloud_ssh_key.ssh_key.id]

  data_volume = data.hcloud_volume.data.id

  backup_s3_bucket     = data.aws_s3_bucket.backup.id
  backup_s3_access_key = var.backup_s3_access_key
  backup_s3_secret_key = var.backup_s3_secret_key

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
```

### [Solidblocks Shell](https://pellepelster.github.io/solidblocks/shell/)

Reusable shell functions for infrastructure automation and developer experience

* [Download](https://pellepelster.github.io/solidblocks/shell/download/) Secure and reliable data retrieval from remote servers
* [File](https://pellepelster.github.io/solidblocks/shell/file/) Utilities for local file operations
* [Software](https://pellepelster.github.io/solidblocks/shell/software/) Tooling setup for local development and continuous integration environments
* [Log](https://pellepelster.github.io/solidblocks/shell/log/) Generic console and logging and helpers
* [Text](https://pellepelster.github.io/solidblocks/shell/text/) Constants for console text formatting
* [AWS](https://pellepelster.github.io/solidblocks/shell/aws/) Utilities for the AWS cloud API
* [Terraform](https://pellepelster.github.io/solidblocks/shell/terraform/) Wrappers and helpers for Terraform
* [Python](https://pellepelster.github.io/solidblocks/shell/python/) Wrappers and helpers for Python


#### Usage example

```shell
#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

SOLIDBLOCKS_SHELL_VERSION="v0.1.15"
SOLIDBLOCKS_SHELL_CHECKSUM="12be1afac8ba2166edfa9eb01ca984aa7c1db4350cd8653a711394a22c3b599a"

# self contained function for initial Solidblocks bootstrapping
function bootstrap_solidblocks() {
  local default_dir="$(cd "$(dirname "$0")" ; pwd -P)"
  local install_dir="${1:-${default_dir}/.solidblocks-shell}"

  local temp_file="$(mktemp)"

  curl -v -L "${SOLIDBLOCKS_BASE_URL:-https://github.com}/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_SHELL_VERSION}/solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" > "${temp_file}"
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
  source "${DIR}/.solidblocks-shell/text.sh"
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

function task_log {
    log_info "info message"
    log_success "success message"
    log_warning "warning message"
    log_debug "debug message"
    log_error "error message"
}

function task_text {
    echo "${FORMAT_DIM}Dim${FORMAT_RESET}"
    echo "${FORMAT_UNDERLINE}Underline${FORMAT_RESET}"
    echo "${FORMAT_BOLD}Bold${FORMAT_RESET}"
    echo "${COLOR_RED}Red${COLOR_RESET}"
    echo "${COLOR_GREEN}Green${COLOR_RESET}"
    echo "${COLOR_YELLOW}Yellow${COLOR_RESET}"
    echo "${COLOR_BLACK}Black${COLOR_RESET}"
    echo "${COLOR_BLUE}Blue${COLOR_RESET}"
    echo "${COLOR_MAGENTA}Magenta${COLOR_RESET}"
    echo "${COLOR_CYAN}Cyan${COLOR_RESET}"
    echo "${COLOR_WHITE}White${COLOR_RESET}"
}

# provide some meaningful help using shell formatting from https://pellepelster.github.io/solidblocks/shell/text/
function task_usage {
  cat <<EOF
Usage: $0

  bootstrap             initialize the development environment
  terraform             run terraform
  log                   log some stuff
  text                  print soe fancy text formats
EOF
  exit 1
}

ARG=${1:-}
shift || true

# if we see the bootstrap command assume Solidshell is not yet initialized and skip environment setup
case "${ARG}" in
  bootstrap) ;;
  *) ensure_environment ;;
esac

case ${ARG} in
  bootstrap) task_bootstrap "$@" ;;
  terraform) task_terraform "$@" ;;
  log)       task_log "$@" ;;
  text)      task_text "$@" ;;
  *) task_usage ;;
esac
```
