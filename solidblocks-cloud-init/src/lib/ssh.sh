function ssh_write_host_identity() {
  echo "[=ssh_identity_ed25519_key]" | base64 -d > /etc/ssh/ssh_host_ed25519_key
  chmod 600 /etc/ssh/ssh_host_ed25519_key
  echo "[=ssh_identity_ed25519_pub]" | base64 -d > /etc/ssh/ssh_host_ed25519_key.pub
}

function create_root_ssh_key() {
  local ssh_dir="/root/.ssh"
  local ssh_private_key="${ssh_dir}/id_ed25519"

  mkdir -p "${ssh_dir}" || true
  ssh-keygen -t ed25519 -f ${ssh_private_key} -q -N ""
}