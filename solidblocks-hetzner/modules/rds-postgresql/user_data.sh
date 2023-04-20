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

function rds_service_systemd_backup_config {
local backup_type="$${1:-}"
cat <<-EOF
[Unit]
Description=full backup for %i

[Service]
Type=oneshot
WorkingDirectory=/opt/dockerfiles/%i
ExecStart=/usr/bin/docker-compose exec -T ${db_instance_name} /rds/bin/backup-$${backup_type}.sh
EOF
}

function rds_service_systemd_backup_timer {
local backup_type="$${1:-}"
local backup_calendar="$${2:-}"
cat <<-EOF
[Unit]
Description=full backup for %i

[Timer]
OnCalendar=$${backup_calendar}

Unit=rds-backup-$${backup_type}@%i.service
[Install]
WantedBy=multi-user.target
EOF
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
  ${db_instance_name}:
    image: ghcr.io/pellepelster/solidblocks-rds-postgresql:${solidblocks_version}
    environment:
      - "DB_INSTANCE_NAME=${db_instance_name}"
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

mkdir -p "/opt/dockerfiles/${db_instance_name}"
docker_compose_config > "/opt/dockerfiles/${db_instance_name}/docker-compose.yml"

rds_service_systemd_config > /etc/systemd/system/rds@.service
rds_service_systemd_backup_config "full" > /etc/systemd/system/rds-backup-full@.service
rds_service_systemd_backup_timer "full" "${backup_full_calendar}" > /etc/systemd/system/rds-backup-full@.timer

rds_service_systemd_backup_config "incr" > /etc/systemd/system/rds-backup-incr@.service
rds_service_systemd_backup_timer "incr" "${backup_incr_calendar}" > /etc/systemd/system/rds-backup-incr@.timer

systemctl daemon-reload

systemctl enable rds@${db_instance_name}
systemctl start rds@${db_instance_name}

systemctl enable rds-backup-full@${db_instance_name}.timer
systemctl enable rds-backup-incr@${db_instance_name}.timer
