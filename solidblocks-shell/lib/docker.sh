_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

test -f "${_DIR}/utils.sh" && source "${_DIR}/utils.sh"

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

function docker_install_debian() {
  apt-get update -y
  apt-get install --no-install-recommends --no-install-suggests --yes ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
  echo "1500c1f56fa9e26b9b8f42452a553675796ade0807cdce11975eb98170b3a570  /etc/apt/keyrings/docker.asc" | sha256sum -c
  chmod a+r /etc/apt/keyrings/docker.asc
  tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF
  apt-get update -y
  apt-get install  --no-install-recommends --no-install-suggests --yes docker-ce docker-ce-cli containerd.io docker-compose-plugin
}