function garage_install() {
  curl_wrapper https://garagehq.deuxfleurs.fr/_releases/v2.1.0/x86_64-unknown-linux-musl/garage -o /usr/local/bin/garage
  echo "543b0414d1464ab855ebe9b843938a5e5361fd24436891ce3dff9e03d02839d8 /usr/local/bin/garage" | sha256sum --check
  chmod +x /usr/local/bin/garage
}
