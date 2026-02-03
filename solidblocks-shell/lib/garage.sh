function garage_install() {
  curl_wrapper https://garagehq.deuxfleurs.fr/_releases/v2.1.0/x86_64-unknown-linux-musl/garage -o /usr/local/bin/garage
  echo "543b0414d1464ab855ebe9b843938a5e5361fd24436891ce3dff9e03d02839d8 /usr/local/bin/garage" | sha256sum --check
  chmod +x /usr/local/bin/garage
}

# can be setup using the SDK after https://git.deuxfleurs.fr/Deuxfleurs/garage/issues/1249 is available upstream
function garage_apply_layout() {
  local size=${1:-}

  while ! garage status; do
    echo "waiting for garage startup"
    sleep 5
  done

  local node_id="$(garage status | grep '127.0.0.1' | awk '{print $1}')"
  garage layout assign -z dc1 -c "${size}G" "${node_id}"
  local apply_command="$(garage layout show | grep 'garage layout apply')"
  ${apply_command}
}