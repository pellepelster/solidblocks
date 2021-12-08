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
    tls_cert_file = "/storage/local/vault/config/certificates/live/${SOLIDBLOCKS_HOSTNAME}.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}/fullchain.pem"
    tls_key_file = "/storage/local/vault/config/certificates/live/${SOLIDBLOCKS_HOSTNAME}.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}/privkey.pem"
}

storage "file" {
  path = "/storage/local/vault/data"
}

api_addr = "https://${SOLIDBLOCKS_HOSTNAME}.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200"
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
    --expand \
    --config-dir /storage/local/vault/config/certificates \
    --domain ${SOLIDBLOCKS_HOSTNAME}.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN} \
    --domain vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}
EOF
}


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
