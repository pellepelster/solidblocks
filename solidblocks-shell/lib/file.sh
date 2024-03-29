#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/utils.sh"


# see https://pellepelster.github.io/solidblocks/shell/file/#file_extract_to_directory
function file_extract_to_directory {
    ensure_command "unzip"
    ensure_command "tar"

    local compressed_file=${1}
    local target_dir=${2}

    local completion_marker
    completion_marker=${target_dir}/.$(basename ${compressed_file}).extracted

    if [ -f "${completion_marker}" ]; then
        return
    fi

    mkdir -p "${target_dir}" || true

    echo -n "extracting ${compressed_file}..."
    if [[ ${compressed_file} =~ \.zip$ ]]; then
        unzip -qq -o "${compressed_file}" -d "${target_dir}"
        touch "${completion_marker}"
    else
        tar -xf "${compressed_file}" -C "${target_dir}"
        touch "${completion_marker}"
    fi

    echo "done"
}
