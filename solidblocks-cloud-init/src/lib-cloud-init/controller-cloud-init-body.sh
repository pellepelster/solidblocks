
configure_public_ip

package_update
package_check_and_install "jq"
package_check_and_install "unzip"
package_check_and_install "uuid"

bootstrap_solidblocks

source "${SOLIDBLOCKS_DIR}/lib/solidblocks-node-manager.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul-template.sh"
source "${SOLIDBLOCKS_DIR}/lib/hetzner-api.sh"
source "${SOLIDBLOCKS_DIR}/lib/ssh.sh"

echo "CONTROLLER_NODE_COUNT=[=controller_node_count]" >> "${SOLIDBLOCKS_DIR}/instance/environment"

create_root_ssh_key
consul_template_install
solidblocks_node_manager_install "controller"
