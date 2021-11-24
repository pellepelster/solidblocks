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
  chmod 700 "${SOLIDBLOCKS_DIR}/protected"
  chmod 700 "${SOLIDBLOCKS_DIR}/certificates"

  echo "SOLIDBLOCKS_DEBUG_LEVEL=${SOLIDBLOCKS_DEBUG_LEVEL}" > "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_ENVIRONMENT=${SOLIDBLOCKS_ENVIRONMENT}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_HOSTNAME=$(hostname)" >> "${SOLIDBLOCKS_DIR}/instance/environment"
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
# network.sh                          #
#######################################

function configure_public_ip() {
  ip addr add ${SOLIDBLOCKS_PUBLIC_IP} dev eth0
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
function mount_storage() {

    while [ ! -b "${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE}" ]; do
      echo "waiting for storage device '${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE}'"
      sleep 5
    done

    echo "${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE} ${SOLIDBLOCKS_STORAGE_LOCAL_DIR}   ext4   defaults  0 0" >> /etc/fstab
    mkdir -p "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}"
    mount "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}"
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

VAULT_VERSION="1.3.1"
VAULT_CHECKSUM="b49de8fd508eb9c2c222fa0f38e23546fb28991af2e8bfdb9bbe381a380f9baa"
VAULT_URL="https://releases.hashicorp.com/vault/${VAULT_VERSION}/vault_${VAULT_VERSION}_linux_amd64.zip"

function install_vault() {
    curl -sL "${VAULT_URL}" --output "/tmp/vault.zip"
    echo "${VAULT_CHECKSUM}" "/tmp/vault.zip" | sha256sum --check --quiet
    unzip /tmp/vault.zip -d /usr/local/bin
    rm -f /tmp/vault.zip
}

function vault_systemd_config() {
cat <<-EOF
[Unit]
Description=vault
[Service]
Restart=always
User=root
Group=root
WorkingDirectory=/storage/local/vault
ExecStart=/usr/local/bin/vault server -config /storage/local/vault/config
[Install]
WantedBy=multi-user.target
EOF
}

function vault_config() {
cat <<-EOF
ui = true

listener "tcp" {
    address = "0.0.0.0:8200"
    tls_cert_file = "/storage/local/vault/config/certificates/live/vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}/fullchain.pem"
    tls_key_file = "/storage/local/vault/config/certificates/live/vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}/privkey.pem"
}

storage "file" {
  path = "/storage/local/vault/data"
}

api_addr = "https://[=hostname].${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200"
cluster_addr = "https://vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200"
EOF
}

function certbot_run() {
cat <<-EOF
#!/usr/bin/env bash

certbot certonly \
    --email "kontakt@solidblocks.de" \
    --agree-tos --non-interactive \
    --standalone \
    --preferred-challenges http \
    --config-dir /storage/local/vault/config/certificates \
    --domain vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}
EOF
}

#    --domain [=hostname].${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN} \

configure_public_ip
ssh_write_host_identity
mount_storage

package_update
update_system
package_check_and_install "unzip"
package_check_and_install "curl"
package_check_and_install "jq"
package_check_and_install "certbot"

install_vault

certbot_run > /etc/cron.daily/certbot
chmod +x /etc/cron.daily/certbot
/etc/cron.daily/certbot

mkdir -p /storage/local/vault/config
mkdir -p /storage/local/vault/data

vault_config > /storage/local/vault/config/vault.hcl
vault_systemd_config > /etc/systemd/system/vault.service

systemctl daemon-reload
systemctl enable vault
systemctl start vault
