#######################################
# nomad.sh                           #
#######################################

NOMAD_CHECKSUM="7cf3aaf061144852a960194abb04e462476138b2b5751935ec52d8e50d179da1"
NOMAD_URL="https://releases.hashicorp.com/nomad/0.12.6/nomad_0.12.6_linux_amd64.zip"

function nomad_agent_config {
local role=${1:-}
cat <<-EOF
datacenter = "$(config '.cloud_name')"
name = "$(hostname)"
bind_addr = "$(hetzner_get_own_private_ip)"
log_level = "DEBUG"

client {
    enabled = true
    servers = $(hetzner_controller_private_ips)

    meta {
        role = "${role}"
        task_group_id = "${TASK_GROUP_ID:-none}"
    }

    options {
      "docker.volumes.enabled" = "true"
    }
}

consul {
    address = "127.0.0.1:8500"
    auto_advertise = true
    server_auto_join = true
    client_auto_join = true
    token = "${CONSUL_HTTP_TOKEN}"
}

plugin "raw_exec" {
    config {
        enabled = true
    }
}
EOF
}

function nomad_server_config {
cat <<-EOF
datacenter = "$(config '.cloud_name')"
name = "$(hostname)"
bind_addr = "$(hetzner_get_own_private_ip)"
log_level = "DEBUG"

server {
    enabled          = true
    bootstrap_expect = ${CONTROLLER_NODE_COUNT}
}

client {
    enabled = true
    meta {
        role = "controller"
    }
}

addresses {
  http = "$(hetzner_get_own_private_ip)"
}

plugin "raw_exec" {
  config {
    enabled = true
  }
}

consul {
    address = "127.0.0.1:8500"
    auto_advertise = true
    server_auto_join = true
    client_auto_join = true
    token = "$(config '.consul_master_token')"
}

autopilot {
    cleanup_dead_servers = true
    last_contact_threshold = "200ms"
    max_trailing_logs = 250
    server_stabilization_time = "10s"
    enable_redundancy_zones = false
    disable_upgrade_migration = false
    enable_custom_upgrades = false
}
EOF
}

function nomad_agent_systemd_config() {
cat <<-EOF
[Unit]
Description=nomad server
Requires=network-online.target
After=network-online.target

[Service]
User=root
EnvironmentFile=-/etc/sysconfig/nomad
Restart=on-failure
ExecStart=/usr/local/bin/nomad agent -config=/etc/nomad -data-dir=${STORAGE_LOCAL_DIR}/nomad
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function nomad_server_systemd_config() {
cat <<-EOF
[Unit]
Description=nomad server
Requires=network-online.target
After=network-online.target

[Service]
User=root
EnvironmentFile=-/etc/sysconfig/nomad
Restart=on-failure
ExecStart=/usr/local/bin/nomad agent -config=/etc/nomad -data-dir=${STORAGE_LOCAL_DIR}/nomad
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function nomad_install() {
    local target_file="/tmp/nomad.zip"
    download_and_verify_checksum "${NOMAD_URL}" "${target_file}" "${NOMAD_CHECKSUM}"
    unzip -o -d /usr/local/bin ${target_file}

    curl_wrapper -o /tmp/cni-plugins.tgz https://github.com/containernetworking/plugins/releases/download/v0.8.1/cni-plugins-linux-amd64-v0.8.1.tgz
    sudo mkdir -p /opt/cni/bin
    sudo tar -C /opt/cni/bin -xzf /tmp/cni-plugins.tgz
    rm /tmp/cni-plugins.tgz

    modprobe br_netfilter

    echo 1 > /proc/sys/net/bridge/bridge-nf-call-arptables
    echo 1 > /proc/sys/net/bridge/bridge-nf-call-ip6tables
    echo 1 > /proc/sys/net/bridge/bridge-nf-call-iptables

    # shellcheck disable=SC2129
    echo "net.bridge.bridge-nf-call-arptables = 1" >> /etc/sysctl.d/99-sysctl.conf
    # shellcheck disable=SC2129
    echo "net.bridge.bridge-nf-call-ip6tables = 1" >> /etc/sysctl.d/99-sysctl.conf
    echo "net.bridge.bridge-nf-call-iptables = 1" >> /etc/sysctl.d/99-sysctl.conf
}

function nomad_server_bootstrap() {
    nomad_install
    nomad_init_env

    nomad_server_config > /etc/nomad/nomad-server.json
    nomad_server_systemd_config > /etc/systemd/system/nomad-server.service

    systemctl daemon-reload
    systemctl restart nomad-server
}

function nomad_init_env() {
    mkdir -p /etc/nomad
    mkdir -p "${STORAGE_LOCAL_DIR}/nomad"

    useradd --no-create-home "nomad"

    chown nomad "${STORAGE_LOCAL_DIR}/nomad"
}

function nomad_agent_bootstrap() {
    local role=${1:-}
    nomad_install
    nomad_init_env

    nomad_agent_config "${role}" > /etc/nomad/nomad-agent.json
    nomad_agent_systemd_config > /etc/systemd/system/nomad-agent.service

    systemctl daemon-reload
    systemctl restart nomad-agent
}
