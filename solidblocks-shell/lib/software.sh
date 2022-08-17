#!/usr/bin/env bash

shopt -s globstar

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/download.sh"
source "${_DIR}/file.sh"

BIN_DIR="${BIN_DIR:-$_DIR/.bin}"
CACHE_DIR="${CACHE_DIR:-$_DIR/.cache}"

###########################################
#              shellcheck                 #
###########################################

SHELLCHECK_VERSION="v0.8.0"
SHELLCHECK_CHECKSUM="ab6ee1b178f014d1b86d1e24da20d1139656c8b0ed34d2867fbb834dad02bf0a"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_shellcheck
function software_ensure_shellcheck() {
  local version=${1:-$SHELLCHECK_VERSION}
  local checksum=${2:-$SHELLCHECK_CHECKSUM}

  download_and_verify_checksum "https://github.com/koalaman/shellcheck/releases/download/${version}/shellcheck-${version}.linux.x86_64.tar.xz" "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" "${BIN_DIR}"
  touch "${BIN_DIR}/shellcheck-${version}/export.path"
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

  download_and_verify_checksum "https://github.com/maykonlf/semver-cli/releases/download/${version}/semver-linux-amd64.zip" "${CACHE_DIR}/semver-linux-amd64_${version}.zip" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/semver-linux-amd64_${version}.zip" "${BIN_DIR}"
}

###########################################
#                 hugo                    #
###########################################

HUGO_VERSION="0.101.0"
HUGO_CHECKSUM="3a22bf2b467b861afa62bd0cd1c0bbd18e2c95cac0e0b61f3c7c8459c2b313eb"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_hugo
function software_ensure_hugo() {
  local version=${1:-$HUGO_VERSION}
  local checksum=${2:-$HUGO_CHECKSUM}

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

    mkdir -p "${CACHE_DIR}" || true
    mkdir -p "${BIN_DIR}" || true

    local target_file="${CACHE_DIR}/${product}-${version}.zip"
    local url="https://releases.hashicorp.com/${product}/${version}/${product}_${version}_linux_amd64.zip"

    download_and_verify_checksum "${url}" "${target_file}" "${checksum}"
    file_extract_to_directory "${target_file}" "${BIN_DIR}"
}

TERRAFORM_VERSION="1.2.6"
TERRAFORM_CHECKSUM="9fd445e7a191317dcfc99d012ab632f2cc01f12af14a44dfbaba82e0f9680365"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_terraform
function software_ensure_terraform {
  local version=${1:-$TERRAFORM_VERSION}
  local checksum=${2:-$TERRAFORM_CHECKSUM}

  software_hashicorp_ensure "terraform" "${version}" "${checksum}"
}

CONSUL_VERSION="1.12.3"
CONSUL_CHECKSUM="620a47cfba34bdf918b4c3238d22f6318b29403888cfd927c6006a4ac1b1c9f6"

# see https://pellepelster.github.io/solidblocks/shell/software/#software_ensure_consul
function software_ensure_consul {
  local version=${1:-$CONSUL_VERSION}
  local checksum=${2:-$CONSUL_CHECKSUM}

  software_hashicorp_ensure "consul" "${version}" "${checksum}"
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