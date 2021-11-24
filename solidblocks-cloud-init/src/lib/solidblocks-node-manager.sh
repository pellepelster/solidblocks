#######################################
# consul-template.sh                  #
#######################################

function solidblocks_node_manager_install() {
  cp "${SOLIDBLOCKS_DIR}/config/solidblocks-node-manager.service" /etc/systemd/system
  systemctl daemon-reload
  systemctl enable solidblocks-node-manager
  systemctl start solidblocks-node-manager
}

