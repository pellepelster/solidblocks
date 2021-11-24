#######################################
# consul.sh                           #
#######################################

CONSUL_CHECKSUM="78d127e5b8edd310c3f9f89487fb833a5c7bcb4e09cb731a4d39100fc53b38be"
CONSUL_URL="https://releases.hashicorp.com/consul/1.6.2/consul_1.6.2_linux_amd64.zip"
CONSUL_BACKUP_DIR="${SOLIDBLOCKS_STORAGE_LOCAL_DIR}/consul/snapshots"
CONSUL_DATA_DIR="${SOLIDBLOCKS_STORAGE_LOCAL_DIR}/consul/data"

function consul_agent_config {
cat <<-EOF
{
  "datacenter": "$(config '.cloud_name')",
  "node_id": "$(uuid -v5 ns:DNS "$(hostname -s)")",
  "advertise_addr": "$(hetzner_get_own_private_ip)",
  "client_addr": "$(hetzner_get_own_private_ip)",
  "leave_on_terminate": true,
  "retry_join": $(hetzner_controller_private_ips),
  "ca_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem",
  "cert_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).cert.pem",
  "key_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem",
  "addresses": {
    "http": "127.0.0.1",
    "dns": "127.0.0.1"
  },
  "ports": {
     "dns": 8553
  },
  "encrypt": "$(config '.consul_secret')",
  "verify_incoming": true,
  "verify_incoming_https": false,
  "verify_outgoing": true,
  "enable_script_checks": true,
  "acl": {
    "enabled": true,
    "default_policy": "deny",
    "down_policy": "deny",
    "tokens": {
      "agent": "${CONSUL_HTTP_TOKEN}",
      "default": "${CONSUL_DEFAULT_TOKEN}"
    }
  }
}
EOF
}

function consul_server_config {
cat <<-EOF
{
  "datacenter": "${SOLIDBLOCKS_CLOUD}-${SOLIDBLOCKS_ENVIRONMENT}",
  "node_id": "$(uuid -v5 ns:DNS "$(hostname -s)")",
  "server": true,
  "ui": true,
  "advertise_addr": "$(hetzner_get_own_private_ip)",
  "client_addr": "$(hetzner_get_own_private_ip)",
  "leave_on_terminate": true,
  "retry_join": $(hetzner_controller_private_ips),
  "bootstrap_expect": ${CONTROLLER_NODE_COUNT},
  "ca_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem",
  "cert_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).cert.pem",
  "key_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem",
  "verify_incoming": true,
  "verify_incoming_https": false,
  "verify_outgoing": true,
  "addresses": {
    "https": "0.0.0.0",
    "http": "0.0.0.0",
    "dns": "127.0.0.1",
    "grpc": "127.0.0.1"
  },
  "ports": {
    "dns": 8553,
    "http": 8500,
    "https": 8501,
    "grpc": 8502
  },
  "encrypt": "$(vault_read_secret solidblocks/cloud/config/consul | jq -r '.consul_secret')",
  "acl": {
    "enabled": true,
    "default_policy": "deny",
    "down_policy": "deny",
    "tokens": {
      "master": "$(vault_read_secret solidblocks/cloud/config/consul | jq -r '.consul_master_token')",
      "agent": "$(vault_read_secret solidblocks/cloud/config/consul | jq -r '.consul_master_token')"
    }
  },
  "connect": {
    "enabled": true
  }
}
EOF
}

function consul_agent_systemd_config() {
cat <<-EOF
[Unit]
Description=consul server
Requires=network-online.target
After=network-online.target

[Service]
User=consul
EnvironmentFile=-/etc/sysconfig/consul
Restart=on-failure
ExecStart=/usr/local/bin/consul agent -config-dir=/etc/consul -data-dir=${CONSUL_DATA_DIR}
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function consul_server_systemd_config() {
cat <<-EOF
[Unit]
Description=consul server
Requires=network-online.target
After=network-online.target

[Service]
User=consul
EnvironmentFile=-/etc/sysconfig/consul
Restart=on-failure
ExecStart=/usr/local/bin/consul agent -ui -server -config-dir=/etc/consul --data-dir=${CONSUL_DATA_DIR}
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function consul_install() {
    local target_file="$(mktemp)"
    download_and_verify_checksum "${CONSUL_URL}" "${target_file}" "${CONSUL_CHECKSUM}"
    unzip -o -d /usr/local/bin "${target_file}"
    rm -rf "${target_file}"
}

function consul_create_user() {
    local user
    user="consul"
    if [[ ! $(id -u "${user}") ]]; then
        useradd --no-create-home --no-user-group --groups solidblocks "${user}"
    fi
}

function consul_dns_policy() {
cat <<-EOF
{
  "Name": "dns-discovery-read",
  "Description": "dns service and node discovery",
  "Rules": "node_prefix \"\" { policy = \"read\"}, service_prefix \"\" { policy = \"read\" }"
}
EOF
}

function consul_default_token() {
cat <<-EOF
{
   "Description": "default agent token for '$(hostname)'",
   "Policies": [
      {
         "Name": "dns-discovery-read"
      }
   ],
   "Local": false
}
EOF
}

function wait_for_consul_cluster_to_settle() {
    # wait for consul to settle
    until CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN} consul info; do
      echo "waiting for consul cluster to settle"
      sleep 5
    done

    until CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN:-$(config .consul_master_token)} consul lock solidblocks/instance/test whoami; do
      echo "waiting for consul cluster to settle (lock)"
      sleep 5
    done
}

function consul_server_bootstrap() {
    consul_install
    consul_create_user

    if [[ -v SOLIDBLOCKS_BACKUP_RESTORE ]]; then
        if [[ -d ${CONSUL_DATA_DIR} ]]; then
            mv "${CONSUL_DATA_DIR}" "${CONSUL_DATA_DIR}.$(date +%Y%m%d%H%M%S)"
        fi
    fi

    consul_init_env

    consul_server_config > /etc/consul/consul-server.json
    chown root:solidblocks /etc/consul/consul-server.json
    chmod 750 /etc/consul/consul-server.json

    consul_server_systemd_config > /etc/systemd/system/consul-server.service

    systemctl daemon-reload
    systemctl restart consul-server

    until [[ $(curl -s http://127.0.0.1:8500/v1/status/peers | jq length) -eq ${CONTROLLER_NODE_COUNT} ]]; do
      echo "waiting for consul cluster"
      sleep 5
    done

    wait_for_consul_cluster_to_settle

    if [[ -v SOLIDBLOCKS_BACKUP_RESTORE ]] && [[ $(backup_info "$(backup_url)") ]]; then
        if CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN:-$(config .consul_master_token)} consul info | grep -q "leader = true"; then
            backup_restore "node_$(hostname)_management_consul_*" "${CONSUL_BACKUP_DIR}"

            if [[ -f "${CONSUL_BACKUP_DIR}/consul.snapshot" ]]; then
                CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN:-$(config .consul_master_token)} consul snapshot restore "${CONSUL_BACKUP_DIR}/consul.snapshot"
            fi
            consul_kv_put "solidblocks/instance/${SOLIDBLOCKS_INSTANCE_ID}/consul_restart"
        else
            wait_for_consul_key "solidblocks/instance/${SOLIDBLOCKS_INSTANCE_ID}/consul_restart"
        fi

        service consul-server restart
        wait_for_consul_cluster_to_settle
    fi
}

function consul_reload() {
    CONSUL_HTTP_TOKEN=$(config '.consul_master_token') consul reload
}

function consul_init_env() {
    mkdir -p /etc/consul

    mkdir -p "${CONSUL_BACKUP_DIR}"
    chown consul "${CONSUL_BACKUP_DIR}"

    mkdir -p "${CONSUL_DATA_DIR}"
    chown consul "${CONSUL_DATA_DIR}"
}

function consul_agent_bootstrap() {
    consul_install
    consul_create_user
    consul_init_env

    consul_agent_config > /etc/consul/consul-agent.json
    chown root:solidblocks /etc/consul/consul-agent.json
    chmod 750 /etc/consul/consul-agent.json
    consul_agent_systemd_config > /etc/systemd/system/consul-agent.service

    systemctl daemon-reload
    systemctl restart consul-agent

    until [[ $(curl -s http://127.0.0.1:8500/v1/status/peers | jq length) -eq ${CONTROLLER_NODE_COUNT} ]]; do
      echo "waiting for consul cluster"
      sleep 5
    done
}

function consul_agent_set_resolver() {
cat <<-EOF >/etc/resolv.conf
  nameserver 127.0.0.1
EOF
}
