
mount_storage

package_update
package_check_and_install "jq"
package_check_and_install "unzip"
package_check_and_install "uuid"
package_check_and_install "openjdk-17-jre-headless"

bootstrap_solidblocks

source "/solidblocks/lib/configuration.sh"
source "${SOLIDBLOCKS_DIR}/lib/solidblocks-node-manager.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul-template.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul.sh"
source "${SOLIDBLOCKS_DIR}/lib/ssh.sh"
source "${SOLIDBLOCKS_DIR}/lib/curl.sh"
source "${SOLIDBLOCKS_DIR}/lib/package.sh"
source "${SOLIDBLOCKS_DIR}/lib/network.sh"
source "${SOLIDBLOCKS_DIR}/lib/vault.sh"
source "${SOLIDBLOCKS_DIR}/lib/hetzner-api.sh"

create_root_ssh_key

while [ ! -f "${SOLIDBLOCKS_DIR}/protected/environment" ]; do
  echo "waiting for instance environment"
  sleep 5;
done
export $(xargs < "${SOLIDBLOCKS_DIR}/instance/environment")

while [ ! -f "${SOLIDBLOCKS_DIR}/protected/environment" ]; do
  echo "waiting for protected environment"
  sleep 5;
done
export $(xargs < "${SOLIDBLOCKS_DIR}/protected/environment")

export SOLIDBLOCKS_SERVICE="[=solidblocks_service]"
echo "SOLIDBLOCKS_SERVICE=${SOLIDBLOCKS_SERVICE}" >> "${SOLIDBLOCKS_DIR}/instance/environment"


function service_agent_systemd_config() {
cat <<-EOF
[Unit]
Description=${SOLIDBLOCKS_SERVICE} service agent
Requires=network-online.target
After=network-online.target

[Service]
User=${SOLIDBLOCKS_SERVICE}
Restart=on-failure
ExecStart=/solidblocks/bin/solidblocks-agent-wrapper.sh
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

useradd --no-create-home --no-user-group --groups solidblocks "${SOLIDBLOCKS_SERVICE}"
service_agent_systemd_config > "/etc/systemd/system/${SOLIDBLOCKS_SERVICE}.service"

systemctl daemon-reload
systemctl enable "${SOLIDBLOCKS_SERVICE}"
systemctl restart "${SOLIDBLOCKS_SERVICE}"
