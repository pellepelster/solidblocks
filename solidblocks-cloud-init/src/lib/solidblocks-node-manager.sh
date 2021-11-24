#######################################
# solidblocks-node-manager.sh         #
#######################################

function solidblocks_node_manager_install() {
  local role="${1:-}"
  cp "${SOLIDBLOCKS_DIR}/config/solidblocks-${role}-node-manager.service" /etc/systemd/system
  systemctl daemon-reload
  systemctl enable "solidblocks-${role}-node-manager"
  systemctl start "solidblocks-${role}-node-manager"
}

