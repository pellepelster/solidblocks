export STORAGE_LOCAL_NAME="local1"
export STORAGE_LOCAL_MOUNT_DIR="/storage/local"
export STORAGE_LOCAL_DIR="${STORAGE_LOCAL_MOUNT_DIR}/${STORAGE_LOCAL_NAME}"

function storage_mount_if_needed() {
    local mount_dir="${1:-}"

    if ! grep -q "${mount_dir}" /proc/mounts ; then
        echo "mounting '${mount_dir}'"

        while ! mount "${mount_dir}" && ! grep -q "${mount_dir}" /proc/mounts; do
            echo "mounting '${mount_dir}' failed, retrying"
            sleep 5
        done
    else
        echo "'${mount_dir}' already mounted"
    fi
}

function storage_create_mount_if_needed() {
    local mount_dir="${1:-}"
    local fstab_entry="${2:-}"

    if ! grep -q "${mount_dir}" /etc/fstab; then
        echo "adding fstab entry for '${mount_dir}'"
        echo "${fstab_entry}" >> /etc/fstab
    fi
}

function storage_create_dir() {
    local dir="${1:-}"
    if [[ ! -d "${dir}" ]]; then
        echo "creating dir '${dir}'"
        mkdir -p "${dir}"
    fi
}

function storage_wait_for_volume_action() {
  local volume_id="${1:-}"
  local action_id="${2:-}"

  local finished="null"
  local status="null"

  while [[ ${finished} == "null" ]]  && [[ ${status} != "success" ]]; do
    local response
    response="$(hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/volumes/${volume_id}/actions/${action_id}")"
    finished=$(jq '.action.finished' <<< "${response}")
    status=$(jq '.action.status' <<< "${response}")
    echo "waiting for volume action ${action_id} on volume ${volume_id}"
    sleep 2
  done
}

function storage_create_volume() {
  local name="${1:-}"
  local location="${2:-}"
  local size="10"

  echo "creating volume '${name}' in '${location}'"

  local request
  request="$(jq -n \
    --arg name "${name}" \
    --arg location "${location}" \
    --arg size "${size}" \
    '{name: $name, location: $location, size: $size, automount: false }')"

  local response
  response="$(hetzner_api_call --data "${request}" "${HETZNER_CLOUD_API_URL}/v1/volumes")"

  storage_wait_for_volume_action "$(jq '.volume.id' <<< "${response}")" "$(jq '.action.id' <<< "${response}")"

}

function storage_has_volume() {
  local name="${1:-}"
  local response
  response="$(storage_get_volumes "${name}")"

  local total_entries
  total_entries="$(jq '.meta.pagination.total_entries' <<< "${response}")"

  if [[ ${total_entries} -eq 1 ]]; then
    return 0
  else
    return 1
  fi
}

function storage_get_volume_id() {
  local name="${1:-}"
  storage_get_volumes "${name}" | jq '.volumes[0].id'
}

function storage_is_volume_attached() {
  local name="${1:-}"
  local server
  server="$(storage_get_volumes "${name}" | jq '.volumes[0].server')"

  if [[ ${server} == "null" ]]; then
    return 1
  else
    return 0
  fi
}

function storage_get_volumes() {
  local name="${1:-}"
  hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/volumes?name=${name}"
}

function storage_get_server() {
  local name="${1:-}"
  hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?name=${name}" | jq ".servers[0]"
}

function storage_get_volume() {
  local name="${1:-}"
  hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/volumes?name=${name}" | jq ".volumes[0]"
}

function storage_mount_device_if_needed() {
    local device="${1:-}"
    local mount_dir="${2:-}"

    if ! grep -q "${device}" /proc/mounts ; then
        while ! grep -q "${mount_dir}" "/proc/mounts" ; do
            echo "mounting '${device}' to '${mount_dir}' failed"
            mount "${device}" "${mount_dir}" || true
            sleep 5
        done
    else
        echo "'${device}' already mounted"
    fi
}


function storage_get_device() {
  local name="${1:-}"
  hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/volumes?name=${name}" | jq -r ".volumes[0].linux_device"
}

function storage_ensure_device_initialized() {
  local device="${1:-}"

  #while [ ! -b "${device}" ]; do
  #  echo "waiting for device '${device}'"
  #  sleep 2
  #done

  if blkid --match-token TYPE=ext4 "${device}"; then
      echo "device '${device}' is already formatted with ext4"
  else
      echo "formatting '${device}' with ext4"
      mkfs.ext4 "${device}"
  fi
}

function storage_ensure_device_mounted() {
  local linux_device="${1:-}"
  local mount_dir="${2:-}"
  local device

  storage_create_dir "${mount_dir}"
  storage_ensure_device_initialized "${linux_device}"
  storage_mount_device_if_needed "${linux_device}" "${mount_dir}"
}

function storage_detach_volume() {
  local name="${1:-}"
  local volume_id
  volume_id="$(storage_get_volume_id "${name}")"
  local response
  response="$(hetzner_api_call -X POST "${HETZNER_CLOUD_API_URL}/v1/volumes/${volume_id}/actions/detach")"
  echo "detaching volume '${name}'"
  storage_wait_for_volume_action "${volume_id}" "$(jq '.action.id' <<< "${response}")"
}

function storage_task_storage_dir() {
  local service_id="${1:-}"
  local task_id="${2:-}"
  echo "/storage/services/${service_id}/tasks/${task_id}"
}

function storage_attach_volume() {
  local server_id="${1:-}"
  local volume_id="${2:-}"

  echo "attaching volume ${volume_id} to server ${server_id}"

  local request
  request="$(jq -n \
    --arg server_id "${server_id}" \
    '{server: $server_id, automount: false }')"

  local response

  response="$(hetzner_api_call --data "${request}" "${HETZNER_CLOUD_API_URL}/v1/volumes/${volume_id}/actions/attach")"
  local error
  error="$(jq '.error.code' <<< "${response}")"

  while [[ ${error} != "null" ]] && [[ ${error} != "volume_already_attached" ]]; do
    echo "error attaching volume '${volume_id}', retrying..."
    sleep 5
    response="$(hetzner_api_call --data "${request}" "${HETZNER_CLOUD_API_URL}/v1/volumes/${volume_id}/actions/attach")"
  done

  storage_wait_for_volume_action "${volume_id}" "$(jq '.action.id' <<< "${response}")"
}
