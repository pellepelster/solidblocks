#######################################
# cloud-init-backup.sh                #
#######################################

curl_wrapper "https://seed.[=cloud_name].[=root_domain]/solidblocks-controller_${SOLIDBLOCKS_VERSION}.tar.gz" --output "/tmp/solidblocks-controller_${SOLIDBLOCKS_VERSION}.tar.gz" > /dev/null
(
    cd "${SOLIDBLOCKS_DIR}" || exit 1
    tar -xzvf "/tmp/solidblocks-controller_${SOLIDBLOCKS_VERSION}.tar.gz"
)

export CONSUL_HTTP_TOKEN
CONSUL_HTTP_TOKEN="$(config '.consul_master_token')"
echo "CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

check_and_install "borgbackup"

storage_ensure_device_mounted "${STORAGE_LOCAL_DEVICE}" "${STORAGE_LOCAL_MOUNT_DIR}"
echo "STORAGE_LOCAL_DIR=${STORAGE_LOCAL_DIR}" >> "/solidblocks/instance/environment"

node_install_node_manager

#consul_server_bootstrap
#consul_kv_put "solidblocks/instance_id" "${SOLIDBLOCKS_INSTANCE_ID}"

#dnsmasq_controller_bootstrap
#backup_init_if_needed
#bootstrap_docker_manager
#nomad_server_bootstrap
#network_firewall_controller_setup
#solidblocks_management_bootstrap "controller" "${CONSUL_HTTP_TOKEN}"

echo "done"