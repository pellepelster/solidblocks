#######################################
# network.sh                          #
#######################################

function configure_public_ip() {
  ip addr add ${SOLIDBLOCKS_PUBLIC_IP} dev eth0
}
