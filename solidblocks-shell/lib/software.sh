shopt -s globstar

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/download.sh"
source "${_DIR}/file.sh"

BIN_DIR="${BIN_DIR:-$_DIR/.bin}"
CACHE_DIR="${CACHE_DIR:-$_DIR/.cache}"

function software_ensure_dirs() {
  mkdir -p "${CACHE_DIR}" || true
  mkdir -p "${BIN_DIR}" || true
}

###########################################
#              shellcheck                 #
###########################################

SHELLCHECK_VERSION="v0.8.0"
SHELLCHECK_CHECKSUM="ab6ee1b178f014d1b86d1e24da20d1139656c8b0ed34d2867fbb834dad02bf0a"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_shellcheck
function software_ensure_shellcheck() {
  local version=${1:-$SHELLCHECK_VERSION}
  local checksum=${2:-$SHELLCHECK_CHECKSUM}

  software_ensure_dirs

  download_and_verify_checksum "https://github.com/koalaman/shellcheck/releases/download/${version}/shellcheck-${version}.linux.x86_64.tar.xz" "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" "${BIN_DIR}"
  touch "${BIN_DIR}/shellcheck-${version}/export.path"
}


###########################################
#                s3cmd                    #
###########################################

S3CMD_VERSION="2.4.0"
S3CMD_CHECKSUM="2df6c0797637fe23b392bdbe865be81643f200333b4dc73d3da737a4c6a1a338"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_s3cmd
function software_ensure_s3cmd() {
  local version=${1:-$S3CMD_VERSION}
  local checksum=${2:-$S3CMD_CHECKSUM}

  software_ensure_dirs

  download_and_verify_checksum "https://github.com/s3tools/s3cmd/releases/download/v${version}/s3cmd-${version}.zip" "${CACHE_DIR}/s3cmd-${version}.zip" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/s3cmd-${version}.zip" "${BIN_DIR}"
  touch "${BIN_DIR}/s3cmd-${version}/export.path"
}

###########################################
#                semver                   #
###########################################

SEMVER_VERSION="v1.1.0"
SEMVER_CHECKSUM="03c3ad1dfb84c0671e537a6b91d48167eabc50dbd7a7c26c3fadee1c92079f46"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_semver
function software_ensure_semver() {
  local version=${1:-$SEMVER_VERSION}
  local checksum=${2:-$SEMVER_CHECKSUM}

  software_ensure_dirs

  download_and_verify_checksum "https://github.com/maykonlf/semver-cli/releases/download/${version}/semver-linux-amd64.zip" "${CACHE_DIR}/semver-linux-amd64_${version}.zip" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/semver-linux-amd64_${version}.zip" "${BIN_DIR}"
}

###########################################
#                 hugo                    #
###########################################

HUGO_VERSION="0.145.0"
HUGO_CHECKSUM="5be7b7d5026d01515685a8aad1ca015133c0569688819f139ae9fb21e33fd9aa"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_hugo
function software_ensure_hugo() {
  local version=${1:-$HUGO_VERSION}
  local checksum=${2:-$HUGO_CHECKSUM}

  software_ensure_dirs

  download_and_verify_checksum "https://github.com/gohugoio/hugo/releases/download/v${version}/hugo_${version}_Linux-64bit.tar.gz" "${CACHE_DIR}/hugo_${version}_Linux-64bit.tar.gz" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/hugo_${version}_Linux-64bit.tar.gz" "${BIN_DIR}"
}

###########################################
#              hashicorp                  #
###########################################

# see https://pellepelster.github.io/solidblocks/shell/software/#software_hashicorp_ensure
function software_hashicorp_ensure {
    local product="${1:-}"
    local version="${2:-}"
    local checksum="${3:-}"

    software_ensure_dirs

    local target_file="${CACHE_DIR}/${product}-${version}.zip"
    local url="https://releases.hashicorp.com/${product}/${version}/${product}_${version}_linux_amd64.zip"

    download_and_verify_checksum "${url}" "${target_file}" "${checksum}"
    file_extract_to_directory "${target_file}" "${BIN_DIR}"
}

TERRAFORM_VERSION="1.14.2"
TERRAFORM_CHECKSUM="8314673d57e9fb8e01bfc98d074f51f7efb6e55484cfb2b10baed686de2190da"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_terraform
function software_ensure_terraform {
  local version=${1:-$TERRAFORM_VERSION}
  local checksum=${2:-$TERRAFORM_CHECKSUM}

  software_ensure_dirs

  software_hashicorp_ensure "terraform" "${version}" "${checksum}"
}

###########################################
#                github                   #
###########################################

# see https://pellepelster.github.io/solidblocks/shell/software/#software_github_ensure_bin
function software_github_ensure_bin {
    local user="${1:-}"
    local repository="${2:-}"
    local version="${3:-}"
    local bin_name="${4:-}"
    local checksum="${5:-}"

    software_ensure_dirs

    local target_file="${BIN_DIR}/${bin_name}"
    local url="https://github.com/${user}/${repository}/releases/download/v${version}/${bin_name}_linux_amd64"

    download_and_verify_checksum "${url}" "${target_file}" "${checksum}"
    chmod +x "${target_file}"
}

###########################################
#              terragrunt                 #
###########################################

TERRAGRUNT_VERSION="0.43.0"
TERRAGRUNT_CHECKSUM="e5ceb5ff1b0871a5836cd4877793828cd2baeec0b89bbd3b5c1b0e5150c0f017"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_terragrunt
function software_ensure_terragrunt {
  local version=${1:-$TERRAGRUNT_VERSION}
  local checksum=${2:-$TERRAGRUNT_CHECKSUM}

  software_ensure_dirs

  software_github_ensure_bin "gruntwork-io" "terragrunt" "${version}" "terragrunt" "${checksum}"
}

###########################################
#                 restic                  #
###########################################

RESTIC_VERSION="0.15.1"
RESTIC_CHECKSUM="3631e3c3833c84ba71f22ea3df20381676abc7476a7f6d14424d9abfada91414"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_restic
function software_ensure_restic {
  local version=${1:-$RESTIC_VERSION}
  local checksum=${2:-$RESTIC_CHECKSUM}

  ensure_command "bunzip2"

  software_ensure_dirs

  download_and_verify_checksum "https://github.com/restic/restic/releases/download/v${RESTIC_VERSION}/restic_${RESTIC_VERSION}_linux_amd64.bz2" "${CACHE_DIR}/restic_${RESTIC_VERSION}_linux_amd64.bz2" "${RESTIC_CHECKSUM}"
  bunzip2 --stdout "${CACHE_DIR}/restic_${RESTIC_VERSION}_linux_amd64.bz2" > "${BIN_DIR}/restic"
  chmod +x "${BIN_DIR}/restic"
}

###########################################
#                 path                    #
###########################################

# see https://pellepelster.github.io/solidblocks/shell/software/#software_export_path
function software_export_path() {
  local path=${BIN_DIR}

  for export in ${BIN_DIR}/**/export.path; do
      path="${path}:$(dirname "${export}")"
  done

  echo "${path}"
}

# see https://pellepelster.github.io/solidblocks/shell/software/#software_set_export_path
function software_set_export_path() {
  export PATH="$(software_export_path):${PATH}"
}