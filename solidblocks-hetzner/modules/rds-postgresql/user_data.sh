#!/usr/bin/env bash

set -eu -o pipefail

DB_BACKUP_S3_BUCKET="${db_backup_s3_bucket}"
DB_BACKUP_S3_ACCESS_KEY="${db_backup_s3_access_key}"
DB_BACKUP_S3_SECRET_KEY="${db_backup_s3_secret_key}"

SOLIDBLOCKS_BASE_URL="${solidblocks_base_url}"
STORAGE_DEVICE_DATA="${storage_device_data}"
STORAGE_DEVICE_BACKUP="${storage_device_backup}"

${cloud_init_bootstrap_solidblocks}

bootstrap_solidblocks

function install_prerequisites {
  apt-get install --no-install-recommends -qq -y \
    apparmor \
    docker.io \
    docker-compose \
    ufw \
    uuid
}

function configure_ufw {
  ufw enable
  ufw allow ssh
  ufw allow 5432
}

function rds_service_systemd_config {
cat <<-EOF
[Unit]
Description=rds instance %i
Requires=docker.service
After=docker.service

[Service]
Restart=always
RestartSec=10s

WorkingDirectory=/opt/dockerfiles/%i
ExecStartPre=/usr/bin/docker-compose down -v
ExecStartPre=/usr/bin/docker-compose rm -fv
ExecStartPre=/usr/bin/docker-compose pull

# Compose up
ExecStart=/usr/bin/docker-compose up

# Compose down, remove containers and volumes
ExecStop=/usr/bin/docker-compose down -v

[Install]
WantedBy=multi-user.target
EOF
}

function docker_compose_config {
cat <<-EOF
version: "3"
services:
  ${rds_instance_id}:
    image: ghcr.io/pellepelster/solidblocks-rds-postgresql:${solidblocks_version}
    environment:
      - "DB_INSTANCE_NAME=${rds_instance_id}"
      - "DB_PASSWORD=very-secret"

      - "DB_DATABASE_database1=database1"
      - "DB_USERNAME_database1=user1"
      - "DB_PASSWORD_database1=password1"

      - "DB_BACKUP_S3=1"
      - "DB_BACKUP_S3_BUCKET=${db_backup_s3_bucket}"
      - "DB_BACKUP_S3_ACCESS_KEY=${db_backup_s3_access_key}"
      - "DB_BACKUP_S3_SECRET_KEY=${db_backup_s3_secret_key}"
    ports:
      - "5432:5432"
    volumes:
      - "/storage/data:/storage/data"
      - "/storage/backup:/storage/backup"
EOF
}

groupadd --gid 10000 rds
useradd --gid rds --uid 10000 rds

storage_mount "$${STORAGE_DEVICE_DATA}" "/storage/data"

if [[ -n $${STORAGE_DEVICE_BACKUP} ]]; then
  storage_mount "$${STORAGE_DEVICE_BACKUP}" "/storage/backup"
fi

chown -R rds:rds "/storage"

install_prerequisites
configure_ufw

mkdir -p "/opt/dockerfiles/${rds_instance_id}"
docker_compose_config > "/opt/dockerfiles/${rds_instance_id}/docker-compose.yml"

rds_service_systemd_config > /etc/systemd/system/rds@.service

systemctl daemon-reload
systemctl enable rds@${rds_instance_id}
systemctl start rds@${rds_instance_id}