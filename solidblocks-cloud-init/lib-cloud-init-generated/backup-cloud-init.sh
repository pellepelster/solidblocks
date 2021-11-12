#!/usr/bin/env bash

set -eux
#######################################
# backup-cloud-init-configuration.sh  #
#######################################

export SOLIDBLOCKS_VAULT_TOKEN="${vault_token}"
export SOLIDBLOCKS_ENVIRONMENT="${environment}"
export SOLIDBLOCKS_ROOT_DOMAIN="${root_domain}"
export SOLIDBLOCKS_PUBLIC_IP="${public_ip}"
export SOLIDBLOCKS_VERSION="${solidblocks_version}"

#######################################
# configuration.sh                    #
#######################################

export DEBUG_LEVEL="$${DEBUG_LEVEL:-0}"

export SOLIDBLOCKS_DIR="$${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="$${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="$${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="$${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="$${SOLIDBLOCKS_GROUP:-solidblocks}"

function bootstrap_solidblocks() {

  # shellcheck disable=SC2086
  mkdir -p $${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chmod 700 "$${SOLIDBLOCKS_DIR}/protected"

  local github_owner="pellepelster"
  (
      local config_file="$${SOLIDBLOCKS_DIR}/cloud_init_config.json"
      vault_read_secret "nodes/cloud_init_config" > $${config_file}

      local temp_file="$(mktemp)"

      curl_wrapper -u "$${github_owner}:$(jq -r ".github_token_ro" "$${config_file}")" -L \
        https://maven.pkg.github.com/$${github_owner}/solidblocks/solidblocks/solidblocks-cloud-init/$${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-$${SOLIDBLOCKS_VERSION}.jar > $${temp_file}

      cd "$${SOLIDBLOCKS_DIR}" || exit 1
      unzip $${temp_file}
      rm -rf $${temp_file}
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

function package_check_and_install {
	local package=$${1}
	echo -n "checking if package '$${package}' is installed..."
	if [[ $(dpkg-query -W -f='$${Status}' "$${package}" 2>/dev/null | grep -c "ok installed") -eq 0 ]];
	then
		echo "not found, installing now"
		while ! DEBIAN_FRONTEND="noninteractive" apt-get install --no-install-recommends -qq -y "$${package}"; do
    		echo "installing failed retrying in 30 seconds"
    		sleep 30
    		apt-get update
		done
	else
		echo "ok"
	fi
}

function create_directory_if_needed {
    local directory="$${1}"

    if [[ ! -d "$${directory}" ]]; then
        mkdir -p "$${directory}"
    fi
}

function download_and_verify_checksum {
    local url=$${1}
    local target_file=$${2}
    local checksum=$${3}

    if [[ -f "$${target_file}" ]]; then
        local target_file_checksum
        target_file_checksum=$(sha256sum "$${target_file}" | cut -d' ' -f1)
        if [[ "$${target_file_checksum}" = "$${checksum}" ]]; then
            echo "$${url} already downloaded"
            return
        fi
    fi

    create_directory_if_needed "$(dirname "$${target_file}")"

    echo -n "downloading $${url}..."
    curl_wrapper "$${url}" --output "$${target_file}" > /dev/null
    echo "done"


    echo -n "verifying checksum..."
    echo "$${checksum}" "$${target_file}" | sha256sum --check --quiet
    echo "done"
}

#######################################
# network.sh                          #
#######################################

function configure_public_ip() {
  ip addr add $${SOLIDBLOCKS_PUBLIC_IP} dev eth0
}

#######################################
# vault.sh                            #
#######################################

function vault_read_secret() {
  local path="$${1:-}"
  curl_wrapper -H "X-Vault-Token: $${SOLIDBLOCKS_VAULT_TOKEN}" "https://vault.$${SOLIDBLOCKS_ENVIRONMENT}.$${SOLIDBLOCKS_ROOT_DOMAIN}:8200/v1/solidblocks/data/$${path}" | jq .data.data
}



configure_public_ip

package_update
package_check_and_install "jq"
package_check_and_install "unzip"

bootstrap_solidblocks