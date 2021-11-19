#######################################
# network.sh                          #
#######################################

function configure_public_ip() {
  ip addr add [=public_ip] dev eth0
}
