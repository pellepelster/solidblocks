#######################################
# cloud-init-service.sh                #
#######################################

curl_wrapper "https://seed.[=cloud_name].[=root_domain]/solidblocks-service_${SOLIDBLOCKS_VERSION}.tar.gz" --output "/tmp/solidblocks-service_${SOLIDBLOCKS_VERSION}.tar.gz" > /dev/null
(
    cd "${SOLIDBLOCKS_DIR}" || exit 1
    tar -xzvf "/tmp/solidblocks-service_${SOLIDBLOCKS_VERSION}.tar.gz"
)

echo "CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN}" >> "${SOLIDBLOCKS_DIR}/instance/environment"


backup_prepare_script_docker > "/solidblocks/backup-prepare.d/100_docker"
chmod +x "/solidblocks/backup-prepare.d/100_docker"

node_backup_script > "/solidblocks/bin/backup.sh"
chmod +x "/solidblocks/bin/backup.sh"

check_and_install "borgbackup"


node_install_node_manager
node_manager_script > /solidblocks/bin/node_manager.sh

consul_agent_bootstrap

service_ensure_user

dnsmasq_node_bootstrap

bootstrap_docker_worker
nomad_agent_bootstrap "service"
solidblocks_management_bootstrap "service" "${CONSUL_HTTP_TOKEN}"

echo "done"