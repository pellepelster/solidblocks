#!/usr/bin/env bash
set -eux

source /solidblocks/instance/environment
source /solidblocks/lib/configuration.sh
source /solidblocks/lib/config.sh
source /solidblocks/lib/common.sh
source /solidblocks/lib/backup.sh
source /solidblocks/lib/storage.sh
source /solidblocks/lib/hetzner-api.sh
source /solidblocks/lib/curl.sh

function init_or_restore_task_data() {
    local task_storage_dir="$(storage_task_storage_dir "${SERVICE_ID}" "${TASK_ID}")"

    local volume_name="service-${SERVICE_ID}-task-${TASK_ID}"

    local linux_device
    linux_device=$(solidblocks_management volumes create-and-attach  --name "${volume_name}")

    storage_ensure_device_mounted "${linux_device}" "${task_storage_dir}"

    for task_mount in $(echo ${TASK_MOUNTS}); do
        mkdir -p "${task_storage_dir}/${task_mount}"
    done

    chown -R 4000:4000 "${task_storage_dir}"

    backup_restore "node_*_service_${SERVICE_ID}_task_${TASK_ID}_*" "${task_storage_dir}"
}

init_or_restore_task_data

