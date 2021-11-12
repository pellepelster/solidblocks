#######################################
# cloud-init-common-header.sh         #
#######################################

apt-get update
check_and_install "curl"

curl_wrapper "https://seed.[=cloud_name].[=root_domain]/solidblocks-node_${SOLIDBLOCKS_VERSION}.tar.gz" --output "/tmp/solidblocks-node_${SOLIDBLOCKS_VERSION}.tar.gz" > /dev/null

# shellcheck disable=SC2086
mkdir -p ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates,backup-prepare.d,backup.d}
chmod 700 "${SOLIDBLOCKS_DIR}/protected"

(
    cd "${SOLIDBLOCKS_DIR}" || exit 1
    tar -xzvf "/tmp/solidblocks-node_${SOLIDBLOCKS_VERSION}.tar.gz"
)

source /solidblocks/lib/configuration.sh
source /solidblocks/lib/config.sh
source /solidblocks/lib/log.sh
source /solidblocks/lib/curl.sh
source /solidblocks/lib/common.sh
source /solidblocks/lib/network.sh
source /solidblocks/lib/storage.sh
source /solidblocks/lib/node.sh
source /solidblocks/lib/ssh.sh
source /solidblocks/lib/backup.sh
source /solidblocks/lib/consul.sh
source /solidblocks/lib/consul-lib.sh
source /solidblocks/lib/nomad.sh
source /solidblocks/lib/hetzner-api.sh
source /solidblocks/lib/solidblocks-management.sh
source /solidblocks/lib/dnsmasq.sh
source /solidblocks/lib/fluentd.sh
source /solidblocks/lib/service.sh
source /solidblocks/lib/docker.sh
source /solidblocks/lib/solidblocks-config.sh

log_divider_header "environment"
env
log_divider_footer

check_and_install "jq"
check_and_install "unzip"
check_and_install "uuid"
check_and_install "less"

# shellcheck disable=SC2129
echo "SOLIDBLOCKS_INSTANCE_ID=${SOLIDBLOCKS_INSTANCE_ID}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
echo "VAULT_ADDR=${VAULT_ADDR}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
echo "SOLIDBLOCKS_DEVELOPMENT_MODE=${SOLIDBLOCKS_DEVELOPMENT_MODE}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
echo "CLOUD_NAME=${CLOUD_NAME}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
echo "ROOT_DOMAIN=${ROOT_DOMAIN}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
echo "DEBUG_LEVEL=${DEBUG_LEVEL}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

curl_wrapper -H "X-Vault-Token: ${VAULT_TOKEN}" -X GET "${VAULT_ADDR}/v1/${CLOUD_NAME}-kv/data/solidblocks/cloud/config" | jq '.data.data' > "${SOLIDBLOCKS_CONFIG_FILE}"

groupadd "${SOLIDBLOCKS_GROUP}"
chmod -R 750 "${SOLIDBLOCKS_DIR}"
chgrp -R "${SOLIDBLOCKS_GROUP}" "${SOLIDBLOCKS_DIR}"


if [[ -v SOLIDBLOCKS_BACKUP_RESTORE ]]; then
    echo "SOLIDBLOCKS_BACKUP_RESTORE=true" >> "/solidblocks/instance/environment"
fi

touch "${SOLIDBLOCKS_DIR}/protected/vault_environment"
chmod 700 "${SOLIDBLOCKS_DIR}/protected/vault_environment"
echo "VAULT_TOKEN=${VAULT_TOKEN}" >> "${SOLIDBLOCKS_DIR}/protected/vault_environment"
echo "VAULT_ADDR=${VAULT_ADDR}" >> "${SOLIDBLOCKS_DIR}/protected/vault_environment"

consul_template_install

create_ssh_key
enable_ssh_config
echo "root:$(config '.root_password')" | chpasswd
