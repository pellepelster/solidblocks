function caddy_setup() {
  curl -L  https://github.com/caddyserver/caddy/releases/download/v2.11.0-beta.1/caddy_2.11.0-beta.1_linux_amd64.deb -o /tmp/caddy.deb
  echo "f36ad374ca1f74f5849c613b718f0ae2a0615c49a7df98ed9d7d3b60bf4fe1fd /tmp/caddy.deb" | sha256sum --check
  dpkg --install /tmp/caddy.deb
  rm -rf /tmp/caddy.deb

  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/www/logs/
  chown -R caddy:caddy ${BLCKS_STORAGE_MOUNT_DATA}/www

  caddy_config > /etc/caddy/Caddyfile
  systemctl restart caddy
}
