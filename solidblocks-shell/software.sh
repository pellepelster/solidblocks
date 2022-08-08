#!/usr/bin/env bash

shopt -s globstar

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/download.sh"
source "${_DIR}/file.sh"

BIN_DIR="${BIN_DIR:-$_DIR/.bin}"
CACHE_DIR="${CACHE_DIR:-$_DIR/.cache}"

SHELLCHECK_VERSION="v0.8.0"
SHELLCHECK_CHECKSUM="ab6ee1b178f014d1b86d1e24da20d1139656c8b0ed34d2867fbb834dad02bf0a"

function software_ensure_shellcheck() {
  local version=${1:-$SHELLCHECK_VERSION}
  local checksum=${2:-$SHELLCHECK_CHECKSUM}

  download_and_verify_checksum "https://github.com/koalaman/shellcheck/releases/download/${version}/shellcheck-${version}.linux.x86_64.tar.xz" "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" "${BIN_DIR}"
  touch "${BIN_DIR}/shellcheck-${version}/export.path"
}

HUGO_VERSION="0.101.0"
HUGO_CHECKSUM="3a22bf2b467b861afa62bd0cd1c0bbd18e2c95cac0e0b61f3c7c8459c2b313eb"

function software_ensure_hugo() {
  local version=${1:-$HUGO_VERSION}
  local checksum=${2:-$HUGO_CHECKSUM}

  download_and_verify_checksum "https://github.com/gohugoio/hugo/releases/download/v${version}/hugo_${version}_Linux-64bit.tar.gz" "${CACHE_DIR}/hugo_${version}_Linux-64bit.tar.gz" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/hugo_${version}_Linux-64bit.tar.gz" "${BIN_DIR}"
}

: '
Generic wrapper for downloading HasiCorp tools built around the convention that product distributions are available at
https://releases.hashicorp.com/`${product}`/`${version}`/`${product}`_`${product}`_linux_amd64.zip and the downloaded
zip contains an executable named `${product}` which will be written to `${bin_dir}`.
'
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

: '
Downloads terraform `${version}` (download will be checked against `${checksum}`) to `${bin_dir}`.
 If `${version}` and `${checksum}` is ommited a recent-ish version will be downloaded.
'

TERRAFORM_VERSION="1.2.6"
TERRAFORM_CHECKSUM="9fd445e7a191317dcfc99d012ab632f2cc01f12af14a44dfbaba82e0f9680365"

function software_ensure_terraform {
  local version=${1:-$TERRAFORM_VERSION}
  local checksum=${2:-$TERRAFORM_CHECKSUM}

  software_hashicorp_ensure "terraform" "${version}" "${checksum}"
}

: '
Downloads Consul `${version}` (download will be checked against `${checksum}`) to `${bin_dir}`.
 If `${version}` and `${checksum}` is ommited a recent-ish version will be downloaded.
'

CONSUL_VERSION="1.12.3"
CONSUL_CHECKSUM="620a47cfba34bdf918b4c3238d22f6318b29403888cfd927c6006a4ac1b1c9f6"

function software_ensure_consul {
  local version=${1:-$CONSUL_VERSION}
  local checksum=${2:-$CONSUL_CHECKSUM}

  software_hashicorp_ensure "consul" "${version}" "${checksum}"
}


function software_export_path() {
  local path=${BIN_DIR}

  for export in ${BIN_DIR}/**/export.path; do
      path="${path}:$(dirname "${export}")"
  done

  echo "${path}"
}

function software_set_export_path() {
  export PATH="$(software_export_path):${PATH}"
}