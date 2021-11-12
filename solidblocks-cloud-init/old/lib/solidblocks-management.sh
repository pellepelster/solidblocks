#######################################
# solidblocks-management.sh           #
#######################################

function solidblocks_management_consul_config() {
    local role=${1:-}
    local consul_token=${2:-}
cat <<-EOF
{
  "service": {
    "token": "${consul_token}",
    "address": "$(hostname).node.consul",
    "checks": [
      {
        "interval": "10s",
        "http": "http://localhost:8080/api/v1/self/health",
        "timeout": "5s"
      }
    ],
    "name": "${role}-api",
    "port": 8080,
    "tags": ["${role}-api", "http"]
  }
}
EOF
}

function solidblocks_management_systemd_config() {
    local command=${1:-}
cat <<-EOF
[Unit]
Description=solidblocks management
Requires=network-online.target
After=network-online.target

[Service]
EnvironmentFile=/solidblocks/instance/environment
EnvironmentFile=/solidblocks/protected/vault_environment
Restart=always
RestartSec=5
ExecStart=/usr/local/bin/solidblocks-management_${SOLIDBLOCKS_VERSION}.sh ${command}
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function solidblocks_management_bootstrap() {
    local role=${1:-}
    local consul_token=${2:-}
    check_and_install "openjdk-11-jre-headless"
    # https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=894979
    # https://bugs.launchpad.net/ubuntu/+source/ca-certificates-java/+bug/1739631
    sed -i 's/keystore.type=pkcs12/keystore.type = jks/g' /etc/java-11-openjdk/security/java.security
    rm /etc/ssl/certs/java/cacerts
    update-ca-certificates --fresh

    consul kv put "solidblocks/management/version" "${SOLIDBLOCKS_VERSION}"

    wget "https://releases.solidblocks.de/solidblocks-management_${SOLIDBLOCKS_VERSION}.sh" -O "/usr/local/bin/solidblocks-management_${SOLIDBLOCKS_VERSION}.sh"
    chmod +x "/usr/local/bin/solidblocks-management_${SOLIDBLOCKS_VERSION}.sh"
    solidblocks_management_systemd_config "${role}" > /etc/systemd/system/solidblocks-management.service

    solidblocks_management_consul_config "${role}" "${consul_token}" > /etc/consul/service_solidblocks_management.json
    chown root:solidblocks /etc/consul/service_solidblocks_management.json
    chmod 750 /etc/consul/service_solidblocks_management.json

    consul_reload

    systemctl daemon-reload
    
    if [[ ! ${DISABLE_MANAGEMENT} -eq 1 ]];  then
        systemctl enable solidblocks-management
        systemctl restart solidblocks-management
    fi

}


function solidblocks_management() {
  while [ ! -f "/opt/solidblocks-management/bin/solidblocks-management" ]; do
    >&2 echo "waiting for solidblocks management binary"
    sleep 2
  done
  "/opt/solidblocks-management/bin/solidblocks-management" "$@"
}
