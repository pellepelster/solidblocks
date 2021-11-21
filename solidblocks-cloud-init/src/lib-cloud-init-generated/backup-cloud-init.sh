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

#######################################
# backup-cloud-init-variables.sh      #
#######################################

export VAULT_TOKEN="[=vault_token]"

#######################################
# configuration.sh                    #
#######################################

export SOLIDBLOCKS_DEBUG_LEVEL="${SOLIDBLOCKS_DEBUG_LEVEL:-0}"

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"

function bootstrap_solidblocks() {

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chmod 700 "${SOLIDBLOCKS_DIR}/protected"

  echo "SOLIDBLOCKS_DEBUG_LEVEL=${SOLIDBLOCKS_DEBUG_LEVEL}" > "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_ENVIRONMENT=${SOLIDBLOCKS_ENVIRONMENT}" >> "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_CLOUD=${SOLIDBLOCKS_CLOUD}" >> "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_ROOT_DOMAIN=${SOLIDBLOCKS_ROOT_DOMAIN}" >> "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "/solidblocks/instance/environment"
  echo "VAULT_ADDRESS=https://vault.${SOLIDBLOCKS_CLOUD}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200" >> "/solidblocks/instance/environment"

  echo "VAULT_TOKEN=${VAULT_TOKEN}" >> "/solidblocks/protected/environment"


  local github_owner="pellepelster"
  (
      local config_file="${SOLIDBLOCKS_DIR}/config/cloud_init_config.json"
      vault_read_secret "solidblocks/cloud/config" > ${config_file}

      local temp_file="$(mktemp)"

      curl_wrapper -u "${github_owner}:$(jq -r ".github_token_ro" "${config_file}")" -L \
        https://maven.pkg.github.com/${github_owner}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar > ${temp_file}

      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip ${temp_file}
      rm -rf ${temp_file}
  )
}

#######################################
# consul-template.sh                  #
#######################################

CONSUL_TEMPLATE_VERSION="0.19.5"
CONSUL_TEMPLATE_CHECKSUM="e6b376701708b901b0548490e296739aedd1c19423c386eb0b01cfad152162af"
CONSUL_TEMPLATE_URL="https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_linux_amd64.zip"

function consul_template_install() {
    local target_file="$(mktemp)"
    download_and_verify_checksum "${CONSUL_TEMPLATE_URL}" "${target_file}" "${CONSUL_TEMPLATE_CHECKSUM}"
    unzip -o -d /usr/local/bin "${target_file}"
    rm -rf "${target_file}"
}


function ssh_write_host_identity() {
  echo "[=ssh_identity_ed25519_key]" | base64 -d > /etc/ssh/ssh_host_ed25519_key
  chmod 600 /etc/ssh/ssh_host_ed25519_key
  echo "[=ssh_identity_ed25519_pub]" | base64 -d > /etc/ssh/ssh_host_ed25519_key.pub
}

function create_root_ssh_key() {
  local ssh_dir="/root/.ssh"
  local ssh_private_key="${ssh_dir}/id_ed25519"

  mkdir -p "${ssh_dir}" || true
  ssh-keygen -t ed25519 -f ${ssh_private_key} -q -N ""
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
  curl_wrapper -H "X-Vault-Token: ${VAULT_TOKEN}" "https://vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200/v1/${SOLIDBLOCKS_CLOUD}-${SOLIDBLOCKS_ENVIRONMENT}-kv/data/${path}" | jq .data.data
}



configure_public_ip

package_update
package_check_and_install "jq"
package_check_and_install "unzip"

bootstrap_solidblocks

create_root_ssh_key
consul_template_install
