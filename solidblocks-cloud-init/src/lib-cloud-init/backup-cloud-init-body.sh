
configure_public_ip

package_update
package_check_and_install "jq"
package_check_and_install "unzip"
package_check_and_install "uuid"
package_check_and_install "certbot"

bootstrap_solidblocks

source "/solidblocks/lib/configuration.sh"
source "${SOLIDBLOCKS_DIR}/lib/solidblocks-node-manager.sh"
source "${SOLIDBLOCKS_DIR}/lib/consul-template.sh"
source "${SOLIDBLOCKS_DIR}/lib/ssh.sh"
source "${SOLIDBLOCKS_DIR}/lib/minio.sh"

function certbot_run() {
cat <<-EOF
#!/usr/bin/env bash

certbot certonly \
    --email "kontakt@solidblocks.de" \
    --agree-tos --non-interactive \
    --standalone \
    --preferred-challenges http \
    --expand \
    --config-dir ${SOLIDBLOCKS_STORAGE_LOCAL_DIR}/${SOLIDBLOCKS_HOSTNAME}/certbot \
    --domain ${SOLIDBLOCKS_HOSTNAME}.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}
EOF
}

certbot_run > /etc/cron.daily/certbot
chmod +x /etc/cron.daily/certbot
/etc/cron.daily/certbot

create_root_ssh_key
consul_template_install
solidblocks_node_manager_install "backup"


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


minio_bootstrap
