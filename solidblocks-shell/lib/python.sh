#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# see https://pellepelster.github.io/solidblocks/shell/python/#python_ensure_venv
function python_ensure_venv() {
  local dir="${1:-.}"
  local venv_dir="${dir}/${2:-venv}"

  mkdir -p "${venv_dir}"
  local requirements_hash_file="${venv_dir}/requirements.sha256"

  if [[ ! -f "${requirements_hash_file}" ]]; then
    echo "random" | sha256sum > "${requirements_hash_file}"
  fi

  if [[ "$(cat "${dir}/requirements.txt" | sha256sum | cut -f 1 -d " ")" != "$(cat ${requirements_hash_file})" ]]; then
    python3 -m venv "${venv_dir}"
    "${venv_dir}/bin/pip" install -r "${dir}/requirements.txt"
    sha256sum "${dir}/requirements.txt" | cut -f 1 -d " " > "${requirements_hash_file}"
  fi
}
