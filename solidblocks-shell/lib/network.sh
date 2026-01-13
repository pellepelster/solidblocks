_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

test -f "${_DIR}/utils.sh" && source "${_DIR}/utils.sh"

# see https://pellepelster.github.io/solidblocks/shell/network/#network_wait_for_port_open
function network_wait_for_port_open() {
  local host="${1:-}"
  local port="${2:-}"
  local period="${3:-1}"

  log_info "waiting for open port on '${host}:${port}'"
  while ! nc -z ${host} ${port}; do
    log_info "...still waiting for port '${host}:${port}' to open..."
    sleep ${period}
  done
  log_info "port '${host}:${port}' is open"
}
