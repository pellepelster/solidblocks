#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"

function network_wait_for_port() {
  local port=${1:-}
  local host=${2:-localhost}

  while ! nc -z ${host} ${port}; do
    log_echo_err "waiting for port ${port} on '${host}'"
    sleep 1
  done
}
