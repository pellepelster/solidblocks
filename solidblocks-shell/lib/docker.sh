#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/utils.sh"

function docker_mapped_tcp_port() {
  ensure_command "jq"

  local name=${1:-}
  local port=${2:-}

  docker inspect $(docker ps --format '{{.ID}}' --filter "name=${name}") | jq -r ".[0].NetworkSettings.Ports.\"${port}/tcp\"[0].HostPort"
}
