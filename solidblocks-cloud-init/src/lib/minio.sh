function minio_systemd_config() {
cat <<-EOF
[Unit]
Description=minio server
Requires=network-online.target
After=network-online.target

[Service]
User=minio
EnvironmentFile=-/solidblocks/instance/environment
EnvironmentFile=-/solidblocks/protected/environment
Restart=on-failure
ExecStart/usr/local/bin/minio --console-address :8080  /storage/local/minio
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function minio_bootstrap() {
  useradd --no-create-home --no-user-group --groups solidblocks minio
  curl_wrapper https://dl.min.io/server/minio/release/linux-amd64/minio -o /usr/local/bin/minio
  chmod +x /usr/local/bin/minio

  minio_systemd_config > /etc/systemd/system/minio.service

  systemctl daemon-reload
  systemctl enable minio
  systemctl start minio
}
