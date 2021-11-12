function node_manager_systemd_config() {
cat <<-EOF
[Unit]
Description=node-manager
Requires=network-online.target
After=network-online.target

[Service]
EnvironmentFile=-/solidblocks/instance/environment
EnvironmentFile=-/solidblocks/protected/vault_environment
Restart=always
RestartSec=5
ExecStart=/usr/local/bin/consul-template -config /solidblocks/config/node-manager_consul-template-config.hcl
KillSignal=SIGINT

[Install]
WantedBy=multi-user.target
EOF
}

function node_consul_template_config() {
cat <<-EOF
EOF
}

function node_manager_script() {
cat <<-EOF
#!/bin/bash

set -eux

while true; do
	echo "node management ping"
	sleep 5
done
EOF
}

function node_install_node_manager() {

    node_manager_systemd_config > /etc/systemd/system/node-manager.service

    systemctl daemon-reload
    systemctl enable node-manager
    systemctl restart node-manager
}