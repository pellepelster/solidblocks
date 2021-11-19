function ssh_write_host_identity() {
  echo "[=ssh_identity_ed25519_key]" | base64 -d > /etc/ssh/ssh_host_ed25519_key
  chmod 600 /etc/ssh/ssh_host_ed25519_key
  echo "[=ssh_identity_ed25519_pub]" | base64 -d > /etc/ssh/ssh_host_ed25519_key.pub
}
