#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"

NETWORK_WAIT_RETRY_DELAY=${NETWORK_WAIT_RETRY_DELAY:-5}
NETWORK_WAIT_RETRIES=${NETWORK_WAIT_RETRIES:-10}

function network_wait_for_port() {
  local port=${1:-}
  local host=${2:-localhost}
  local retry_delay=${3:-$NETWORK_WAIT_RETRY_DELAY}
  local retries=${4:-$NETWORK_WAIT_RETRIES}

  local try=0
  while [ $try -lt ${retries} ] && ! nc -z "${host}" "${port}"; do
      try=$((try+1))
      log_echo_error "waiting for port ${host}:${port} (${try}/${retries}), retrying in ${retry_delay} seconds"
      sleep "${retry_delay}"
  done
}
