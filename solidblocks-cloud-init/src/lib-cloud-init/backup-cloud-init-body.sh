
configure_public_ip

package_update
package_check_and_install "jq"
package_check_and_install "unzip"

bootstrap_solidblocks

source "${SOLIDBLOCKS_DIR}/lib/solidblocks-node-manager.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul-template.sh"
source "${SOLIDBLOCKS_DIR}/lib/ssh.sh"

create_root_ssh_key
consul_template_install
solidblocks_node_manager_install