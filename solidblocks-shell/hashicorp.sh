#!/usr/bin/env bash

: '
Generic wrapper for downloading HasiCorp tools built around the convention that product distributions are available at
https://releases.hashicorp.com/`${product}`/`${version}`/`${product}`_`${product}`_linux_amd64.zip and the downloaded
zip contains an executable named `${product}` which will be written to `${bin_dir}`.
'
function hashicorp_ensure {
    local product="${1:-}"
    local version="${2:-}"
    local checksum="${3:-}"

    local bin_dir="${4:-~/.bin}"
    local tmp_dir="${5:-/tmp/ctuhl_ensure_terraform.$$}"

    mkdir -p "${tmp_dir}" || true
    mkdir -p "${bin_dir}" || true

    local target_file="${tmp_dir}/${product}-${version}.zip"
    local target_dir="${tmp_dir}/${product}-${version}"
    local url="https://releases.hashicorp.com/${product}/${version}/${product}_${version}_linux_amd64.zip"

    download_and_verify_checksum "${url}" "${target_file}" "${checksum}"
    file_extract_to_directory "${target_file}" "${target_dir}"

    cp "${target_dir}/${product}" "${bin_dir}/${product}"
}

: '
Downloads terraform `${version}` (download will be checked against `${checksum}`) to `${bin_dir}`.
 If `${version}` and `${checksum}` is ommited a recent-ish version will be downloaded.
'
function hashicorp_ensure_terraform {
    local bin_dir="${1:-~/.bin}"
    local version="${2:-0.13.4}"
    local checksum="${3:-a92df4a151d390144040de5d18351301e597d3fae3679a814ea57554f6aa9b24}"
    local tmp_dir="${4:-/tmp/ctuhl_ensure_terraform.$$}"

    hashicorp_ensure "terraform" "${version}" "${checksum}" "${bin_dir}" "${tmp_dir}"
}

: '
Downloads Consul `${version}` (download will be checked against `${checksum}`) to `${bin_dir}`.
 If `${version}` and `${checksum}` is ommited a recent-ish version will be downloaded.
'
function hashicorp_ensure_consul {
    local bin_dir="${1:-~/.bin}"
    local version="${2:-1.8.4}"
    local checksum="${3:-0d74525ee101254f1cca436356e8aee51247d460b56fc2b4f7faef8a6853141f}"
    local tmp_dir="${4:-/tmp/ctuhl_ensure_consul.$$}"

    hashicorp_ensure "consul" "${version}" "${checksum}" "${bin_dir}" "${tmp_dir}"
}
