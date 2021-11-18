#!/usr/bin/env bash

VAULT_VERSION="1.3.1"
VAULT_CHECKSUM="b49de8fd508eb9c2c222fa0f38e23546fb28991af2e8bfdb9bbe381a380f9baa"
VAULT_URL="https://releases.hashicorp.com/vault/${VAULT_VERSION}/vault_${VAULT_VERSION}_linux_amd64.zip"

set -eux

echo "[=ssh_identity_ed25519_key]" | base64 -d > /etc/ssh/ssh_host_ed25519_key
chmod 600 /etc/ssh/ssh_host_ed25519_key
echo "[=ssh_identity_ed25519_pub]" | base64 -d > /etc/ssh/ssh_host_ed25519_key.pub

export DEBIAN_FRONTEND=noninteractive

function mount_storage() {
    echo "[=storage_local_device] /storage/[=hostname]   ext4   defaults  0 0" >> /etc/fstab
    mkdir -p "/storage/[=hostname]"
    mount "/storage/[=hostname]"
}

function configure_public_ip() {
    ip addr add [=public_ip] dev eth0
}

function update_system() {
    apt-get update

    apt-get \
        -o Dpkg::Options::="--force-confnew" \
        --force-yes \
        -fuy \
        dist-upgrade
}

function install_prerequisites() {
    apt-get install --no-install-recommends -qq -y \
        unzip \
        curl \
        certbot
}

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
WorkingDirectory=/storage/[=hostname]/vault
ExecStart=/usr/local/bin/vault server -config /storage/[=hostname]/vault/config
[Install]
WantedBy=multi-user.target
EOF
}

function vault_config() {
cat <<-EOF
ui = true

listener "tcp" {
    address = "0.0.0.0:8200"
    tls_cert_file = "/storage/[=hostname]/vault/config/certificates/live/[=hostname].[=environment_name].[=cloud_root_domain]/fullchain.pem"
    tls_key_file = "/storage/[=hostname]/vault/config/certificates/live/[=hostname].[=environment_name].[=cloud_root_domain]/privkey.pem"
}

storage "file" {
  path = "/storage/[=hostname]/vault/data"
}

api_addr = "https://[=hostname].[=environment_name].[=cloud_root_domain]:8200"
cluster_addr = "https://vault.[=environment_name].[=cloud_root_domain]:8200"
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
    --config-dir /storage/[=hostname]/vault/config/certificates \
    --domain [=hostname].[=environment_name].[=cloud_root_domain] \
    --domain vault.[=environment_name].[=cloud_root_domain]
EOF
}

configure_public_ip

mount_storage
update_system
install_prerequisites
install_vault

certbot_run > /etc/cron.daily/certbot
chmod +x /etc/cron.daily/certbot
/etc/cron.daily/certbot

mkdir -p /storage/[=hostname]/vault/config
mkdir -p /storage/[=hostname]/vault/data

vault_config > /storage/[=hostname]/vault/config/vault.hcl
vault_systemd_config > /etc/systemd/system/vault.service

systemctl daemon-reload
systemctl enable vault
systemctl start vault
