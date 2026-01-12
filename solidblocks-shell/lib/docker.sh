_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/utils.sh"

function docker_mapped_tcp_port() {
  ensure_command "jq"

  local name=${1:-}
  local port=${2:-}

  docker inspect $(docker ps --format '{{.ID}}' --filter "name=${name}") | jq -r ".[0].NetworkSettings.Ports.\"${port}/tcp\"[0].HostPort"
}


function docker_ensure_network() {
  local name=${1:-}

  if [[ -z "$(docker network ls --quiet --filter "Name=${name}")" ]]; then
    log_echo_info "creating docker network '${name}'"
    docker network create "${name}"
  else
    log_echo_info "docker network '${name}' already exists"
  fi

}

function docker_remove_network() {
  local name=${1:-}

  if [[ -n "$(docker network ls --quiet --filter "Name=${name}")" ]]; then
    log_echo_info "removing docker network '${name}'"
    docker network remove "${name}"
  fi

}