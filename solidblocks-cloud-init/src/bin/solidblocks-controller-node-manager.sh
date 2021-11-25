#!/usr/bin/env bash

set -eux

systemctl reload sshd

source "/solidblocks/lib/configuration.sh"
source "${SOLIDBLOCKS_DIR}/lib/curl.sh"
source "${SOLIDBLOCKS_DIR}/lib/package.sh"
source "${SOLIDBLOCKS_DIR}/lib/network.sh"
source "${SOLIDBLOCKS_DIR}/lib/vault.sh"
source "${SOLIDBLOCKS_DIR}/lib/hetzner-api.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul.sh"

export $(xargs < "${SOLIDBLOCKS_DIR}/protected/initial_environment")
export $(xargs < "${SOLIDBLOCKS_DIR}/protected/environment")
export $(xargs < "${SOLIDBLOCKS_DIR}/instance/environment")

consul_server_bootstrap

while true; do
	echo "solidblocks controller node manager ping"
	sleep 30
done
