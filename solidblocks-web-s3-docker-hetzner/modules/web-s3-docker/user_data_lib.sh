function ssh_setup() {
    if [[ -n "${SSH_HOST_KEY_ED25519:-}" ]] && [[ -n "${SSH_HOST_CERT_ED25519:-}" ]]; then
      echo "${SSH_HOST_KEY_ED25519}" > /etc/ssh/ssh_host_ed25519_key
      echo "${SSH_HOST_CERT_ED25519}" > /etc/ssh/ssh_host_ed25519_key.pub
      systemctl restart ssh
    fi
}

function ufw_setup() {
  apt_ensure_package "ufw"
  ufw allow ssh
  ufw allow https
  ufw allow http
  ufw enable
}

