function docker_registry_unit() {
  cat <<EOF
[Unit]
Description=Docker Registry
After=network-online.target
Wants=network-online.target

[Service]
ExecStart=/usr/local/bin/registry serve /etc/docker-registry/config.yml

[Install]
WantedBy=multi-user.target
EOF
}

function docker_registry_config() {
  cat <<EOF
version: 0.1
log:
  accesslog:
    disabled: true
  level: debug
  formatter: text
storage:
  filesystem:
    rootdirectory: ${BLCKS_STORAGE_MOUNT_DATA}/docker-registry
    maxthreads: 100
http:
  addr: :5000
health:
  storagedriver:
    enabled: true
    interval: 10s
    threshold: 3
EOF
}


function docker_registry_setup() {

  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/docker-registry
  mkdir -p /etc/docker-registry

  #chown -R caddy:caddy ${BLCKS_STORAGE_MOUNT_DATA}/www

  curl -L https://github.com/distribution/distribution/releases/download/v3.0.0/registry_3.0.0_linux_amd64.tar.gz > /tmp/registry.tar.gz
  echo "61c9a2c0d5981a78482025b6b69728521fbc78506d68b223d4a2eb825de5ca3d /tmp/registry.tar.gz" | sha256sum --check
  (
    cd /tmp
    tar -xvf /tmp/registry.tar.gz
    mv /tmp/registry /usr/local/bin/registry
    chmod +x /usr/local/bin/registry
    rm /tmp/registry.tar.gz
  )
  docker_registry_config > /etc/docker-registry/config.yml

  docker_registry_unit > /etc/systemd/system/docker-registry.service
  systemctl daemon-reload
  systemctl start docker-registry
}
