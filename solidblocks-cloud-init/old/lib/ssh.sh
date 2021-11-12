#######################################
# ssh.sh                           #
#######################################

function create_ssh_key() {
  local ssh_dir="/root/.ssh"
  local ssh_private_key="${ssh_dir}/id_ed25519"

  mkdir -p "${ssh_dir}" || true
  ssh-keygen -t ed25519 -f ${ssh_private_key} -q -N ""
}

function enable_ssh_config() {
    cp /solidblocks/config/ssh-config/ssh-config.service /etc/systemd/system
    systemctl daemon-reload
    systemctl enable ssh-config
    systemctl restart ssh-config
}
