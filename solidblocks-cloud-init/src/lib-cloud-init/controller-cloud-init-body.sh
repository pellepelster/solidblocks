
configure_public_ip
mount_storage

package_update
package_check_and_install "jq"
package_check_and_install "unzip"
package_check_and_install "uuid"

bootstrap_solidblocks

source "/solidblocks/lib/configuration.sh"
source "${SOLIDBLOCKS_DIR}/lib/solidblocks-node-manager.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul-template.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul.sh"
source "${SOLIDBLOCKS_DIR}/lib/ssh.sh"
source "${SOLIDBLOCKS_DIR}/lib/curl.sh"
source "${SOLIDBLOCKS_DIR}/lib/package.sh"
source "${SOLIDBLOCKS_DIR}/lib/network.sh"
source "${SOLIDBLOCKS_DIR}/lib/vault.sh"
source "${SOLIDBLOCKS_DIR}/lib/hetzner-api.sh"

echo "CONTROLLER_NODE_COUNT=[=controller_node_count]" >> "${SOLIDBLOCKS_DIR}/instance/environment"

create_root_ssh_key
consul_template_install
solidblocks_node_manager_install "controller"

while [ ! -f "${SOLIDBLOCKS_DIR}/protected/environment" ]; do
  echo "waiting for instance environment"
  sleep 5;
done
export $(xargs < "${SOLIDBLOCKS_DIR}/instance/environment")

while [ ! -f "${SOLIDBLOCKS_DIR}/protected/environment" ]; do
  echo "waiting for protected environment"
  sleep 5;
done
export $(xargs < "${SOLIDBLOCKS_DIR}/protected/environment")

#export $(xargs < "${SOLIDBLOCKS_DIR}/protected/environment")

consul_server_bootstrap
