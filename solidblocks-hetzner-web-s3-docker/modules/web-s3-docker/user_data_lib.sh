function ufw_setup() {
  apt_ensure_package "ufw"
  ufw allow ssh
  ufw allow https
  ufw enable
}

