#!/usr/bin/env bash

set -eux

systemctl reload sshd

source "/solidblocks/lib/configuration.sh"
source "${SOLIDBLOCKS_DIR}/lib/curl.sh"
source "${SOLIDBLOCKS_DIR}/lib/package.sh"
source "${SOLIDBLOCKS_DIR}/lib/network.sh"
source "${SOLIDBLOCKS_DIR}/lib/vault.sh"
source "${SOLIDBLOCKS_DIR}/lib/hetzner-api.sh"


while true; do
	echo "solidblocks controller node manager ping"
	sleep 30
done
