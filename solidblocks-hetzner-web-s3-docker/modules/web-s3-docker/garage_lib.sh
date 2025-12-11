function garage_systemd_unit() {
  cat <<EOF
[Unit]
Description=Garage Data Store
After=network-online.target
Wants=network-online.target

[Service]
Environment='RUST_LOG=garage=info' 'RUST_BACKTRACE=1'
ExecStart=/usr/local/bin/garage server
StateDirectory=garage
#DynamicUser=true
#ProtectHome=true
#NoNewPrivileges=true
LimitNOFILE=42000

[Install]
WantedBy=multi-user.target
EOF
}

function garage_setup() {
  local size="${1:-1}"
  curl -L https://garagehq.deuxfleurs.fr/_releases/v2.1.0/x86_64-unknown-linux-musl/garage -o /usr/local/bin/garage
  echo "543b0414d1464ab855ebe9b843938a5e5361fd24436891ce3dff9e03d02839d8 /usr/local/bin/garage" | sha256sum --check
  chmod +x /usr/local/bin/garage
  garage_systemd_unit > /etc/systemd/system/garage.service
  systemctl daemon-reload

  garage_config > /etc/garage.toml
  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/garage/meta
  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/garage/data
  systemctl start garage

  while ! garage status; do
    echo "waiting for garage startup"
    sleep 5
  done

  local node_id="$(garage status | grep '127.0.0.1' | awk '{print $1}')"
  garage layout assign -z dc1 -c "${size}G" "${node_id}"
  local apply_command="$(garage layout show | grep 'garage layout apply')"
  ${apply_command}
}
