#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"

function ensure_command() {
    local command=${1:-}

    if ! type "${command}" &>/dev/null; then
      log_echo_die "command '${command}' not installed"
    fi
}
