#!/usr/bin/env bash

set -eux
#######################################
# cloud-init-variables.sh             #
#######################################

export SOLIDBLOCKS_ENVIRONMENT="[=solidblocks_environment]"
export SOLIDBLOCKS_CLOUD="[=solidblocks_cloud]"
export SOLIDBLOCKS_ROOT_DOMAIN="[=solidblocks_root_domain]"
export SOLIDBLOCKS_PUBLIC_IP="[=solidblocks_public_ip]"
export SOLIDBLOCKS_VERSION="[=solidblocks_version]"
export SOLIDBLOCKS_STORAGE_LOCAL_DEVICE="[=storage_local_device]"
export SOLIDBLOCKS_HOSTNAME="[=solidblocks_hostname]"

#######################################
# vault-cloud-init-variables.sh       #
#######################################

export VAULT_TOKEN="[=vault_token]"
export VAULT_ADDR="[=vault_addr]"

#######################################
# configuration.sh                    #
#######################################

export SOLIDBLOCKS_DEBUG_LEVEL="${SOLIDBLOCKS_DEBUG_LEVEL:-0}"

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"
export SOLIDBLOCKS_STORAGE_LOCAL_DIR="/storage/local"

function bootstrap_solidblocks() {

  groupadd solidblocks

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chmod 770 ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chgrp solidblocks ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}

  echo "SOLIDBLOCKS_DEBUG_LEVEL=${SOLIDBLOCKS_DEBUG_LEVEL}" > "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_ENVIRONMENT=${SOLIDBLOCKS_ENVIRONMENT}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_HOSTNAME=${SOLIDBLOCKS_HOSTNAME}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_CLOUD=${SOLIDBLOCKS_CLOUD}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_ROOT_DOMAIN=${SOLIDBLOCKS_ROOT_DOMAIN}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

  echo "VAULT_ADDR=${VAULT_ADDR}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

  echo "VAULT_TOKEN=${VAULT_TOKEN}" > "${SOLIDBLOCKS_DIR}/protected/initial_environment"
  echo "GITHUB_TOKEN_RO=$(vault_read_secret "solidblocks/cloud/providers/github" | jq -r '.github_token_ro')" >> "${SOLIDBLOCKS_DIR}/protected/initial_environment"
  echo "GITHUB_USERNAME=$(vault_read_secret "solidblocks/cloud/providers/github" | jq -r '.github_username')" >> "${SOLIDBLOCKS_DIR}/protected/initial_environment"

  export $(xargs < "${SOLIDBLOCKS_DIR}/protected/initial_environment")
  (
      local temp_file="$(mktemp)"

      #TODO verify checksum
      curl_wrapper -u "${GITHUB_USERNAME}:${GITHUB_TOKEN_RO}" -L \
        "https://maven.pkg.github.com/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar" > "${temp_file}"

      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip "${temp_file}"
      rm -rf "${temp_file}"
  )
}

#######################################
# curl.sh                             #
#######################################

function curl_wrapper() {
    while ! curl --retry 25 --retry-connrefused --fail --silent --location --show-error "$@"; do
        sleep 5
    done
}

function curl_wrapper_nofail() {
    while ! curl --retry 25 --retry-connrefused --silent --location --show-error "$@"; do
        sleep 5
    done
}
#######################################
# common.sh                           #
#######################################

function package_update {
  apt-get update
}

function update_system() {
    apt-get \
        -o Dpkg::Options::="--force-confnew" \
        --force-yes \
        -fuy \
        dist-upgrade
}

function package_check_and_install {
	local package=${1}
	echo -n "checking if package '${package}' is installed..."
	if [[ $(dpkg-query -W -f='${Status}' "${package}" 2>/dev/null | grep -c "ok installed") -eq 0 ]];
	then
		echo "not found, installing now"
		while ! DEBIAN_FRONTEND="noninteractive" apt-get install --no-install-recommends -qq -y "${package}"; do
    		echo "installing failed retrying in 30 seconds"
    		sleep 30
    		apt-get update
		done
	else
		echo "ok"
	fi
}

function create_directory_if_needed {
    local directory="${1}"

    if [[ ! -d "${directory}" ]]; then
        mkdir -p "${directory}"
    fi
}

function download_and_verify_checksum {
    local url=${1}
    local target_file=${2}
    local checksum=${3}

    if [[ -f "${target_file}" ]]; then
        local target_file_checksum
        target_file_checksum=$(sha256sum "${target_file}" | cut -d' ' -f1)
        if [[ "${target_file_checksum}" = "${checksum}" ]]; then
            echo "${url} already downloaded"
            return
        fi
    fi

    create_directory_if_needed "$(dirname "${target_file}")"

    echo -n "downloading ${url}..."
    curl_wrapper "${url}" --output "${target_file}" > /dev/null
    echo "done"


    echo -n "verifying checksum..."
    echo "${checksum}" "${target_file}" | sha256sum --check --quiet
    echo "done"
}

#######################################
# network.sh                          #
#######################################

function configure_public_ip() {
  ip addr add ${SOLIDBLOCKS_PUBLIC_IP} dev eth0
}

#######################################
# vault.sh                            #
#######################################

function vault_read_secret() {
  local path="${1:-}"
  curl_wrapper -H "X-Vault-Token: ${VAULT_TOKEN}" "${VAULT_ADDR}/v1/${SOLIDBLOCKS_CLOUD}-${SOLIDBLOCKS_ENVIRONMENT}-kv/data/${path}" | jq .data.data
}


function mount_storage() {

    while [ ! -b "${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE}" ]; do
      echo "waiting for storage device '${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE}'"
      sleep 5
    done

    echo "${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE} ${SOLIDBLOCKS_STORAGE_LOCAL_DIR}   ext4   defaults  0 0" >> /etc/fstab
    mkdir -p "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}"
    mount "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}"
}


configure_public_ip
mount_storage

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

chgrp -R solidblocks "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}/${SOLIDBLOCKS_HOSTNAME}/certbot"

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
