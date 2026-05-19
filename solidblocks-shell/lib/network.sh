_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

test -f "${_DIR}/utils.sh" && source "${_DIR}/utils.sh"

# see https://pellepelster.github.io/solidblocks/shell/network/#network_wait_for_port_open
function network_wait_for_port_open() {
  local host="${1:-}"
  local port="${2:-}"
  local period="${3:-1}"

  echo "waiting for open port on '${host}:${port}'"
  while ! nc -z ${host} ${port}; do
    echo "...still waiting for port '${host}:${port}' to open..."
    sleep ${period}
  done
  echo "port '${host}:${port}' is open"
}

function network_add_ipv4() {
  local ip="${1:-}"
  ip addr add ${1:-} dev eth0
  tee /etc/network/interfaces.d/60-floating-ip.cfg <<EOF
auto eth0:1
iface eth0:1 inet static
   address ${ip}
   netmask 32
EOF

}
