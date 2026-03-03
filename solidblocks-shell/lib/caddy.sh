function caddy_install() {
  curl_wrapper https://github.com/caddyserver/caddy/releases/download/v2.11.1/caddy_2.11.1_linux_amd64.deb -o /tmp/caddy.deb
  echo "220ecc579aff4b5ea061ff501d9569e21b4195ee3560a02222952390be73c8ff /tmp/caddy.deb" | sha256sum --check
  dpkg --install /tmp/caddy.deb
  rm -rf /tmp/caddy.deb
}
