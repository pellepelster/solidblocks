#!/usr/bin/env bash

shopt -s globstar

DIR="${DIR:-cd "$(dirname "$0")" ; pwd -P}"

BIN_DIR="${BIN_DIR:-$DIR/.bin}"
CACHE_DIR="${CACHE_DIR:-$DIR/.cache}"

function software_ensure_shellcheck() {
  local version="v0.8.0"
  local checksum="ab6ee1b178f014d1b86d1e24da20d1139656c8b0ed34d2867fbb834dad02bf0a"

  download_and_verify_checksum "https://github.com/koalaman/shellcheck/releases/download/${version}/shellcheck-${version}.linux.x86_64.tar.xz" "${CACHE_DIR}/shellcheck-${version}.linux.x86_64.tar.xz" ${checksum}
  file_extract_to_directory "${DIR}/.cache/shellcheck-${version}.linux.x86_64.tar.xz" "${BIN_DIR}"
  touch "${BIN_DIR}/shellcheck-${version}/export.path"
}

function software_ensure_hugo() {
  local version="0.101.0"
  local checksum="3a22bf2b467b861afa62bd0cd1c0bbd18e2c95cac0e0b61f3c7c8459c2b313eb"

  download_and_verify_checksum "https://github.com/gohugoio/hugo/releases/download/v${version}/hugo_${version}_Linux-64bit.tar.gz" "${CACHE_DIR}/hugo_${version}_Linux-64bit.tar.gz" ${checksum}
  file_extract_to_directory "${CACHE_DIR}/hugo_${version}_Linux-64bit.tar.gz" "${BIN_DIR}"
  #touch "${BIN_DIR}/shellcheck-${version}/export.path"
}

function software_export_path() {
  local path=${BIN_DIR}

  for export in ${BIN_DIR}/**/export.path; do
      path="${path}:$(dirname "${export}")"
  done

  echo "${path}"
}

function software_ensure_export_path() {
  export PATH="${PATH}:$(software_export_path)"
}