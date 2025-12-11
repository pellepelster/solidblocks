#!/usr/bin/env bash

set -eu -o pipefail

${variables}
${storage_lib}
${apt_lib}
${caddy_lib}
${caddy}
${garage}
${garage_lib}
${docker_registry_lib}
${user_data_lib}

echo "${s3_buckets_json_base64}" | base64 -d > /root/s3_buckets.json
echo "${garage_py_base64}" | base64 -d > /root/garage.py
echo "${requirements_txt_base64}" | base64 -d > /root/requirements.txt

storage_mount "$${BLCKS_STORAGE_DEVICE_DATA}" "$${BLCKS_STORAGE_MOUNT_DATA}"
apt_update_repositories
caddy_setup
garage_setup "${data_volume_size}"
garage_provision
docker_registry_setup
ufw_setup