function caddy_setup() {
  apt_ensure_package "caddy"

  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/www/logs/
  chown -R caddy:caddy ${BLCKS_STORAGE_MOUNT_DATA}/www

  caddy_config > /etc/caddy/Caddyfile
  systemctl restart caddy
}
