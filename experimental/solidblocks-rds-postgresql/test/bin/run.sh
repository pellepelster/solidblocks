#!/usr/bin/env bash

set -eu

if [[ -z "${BACKUP_HOST_PRIVATE_KEY:-}" ]]; then
  echo "BACKUP_HOST_PRIVATE_KEY not set"
  exit 1
fi

if [[ -z "${DB_SSH_PUBLIC_KEY:-}" ]]; then
  echo "DB_SSH_PUBLIC_KEY not set"
  exit 1
fi

gomplate --input-dir /pgbackrest/templates/config/ --output-map='/pgbackrest/config/{{ .in | strings.ReplaceAll ".template" "" }}'

touch /pgbackrest/sshd_host_ed25519
chmod 600 /pgbackrest/sshd_host_ed25519
echo "${BACKUP_HOST_PRIVATE_KEY}" >> /pgbackrest/sshd_host_ed25519

mkdir ~pgbackrest/.ssh
echo "${DB_SSH_PUBLIC_KEY}" >> ~pgbackrest/.ssh/authorized_keys
chmod 750 -R ~pgbackrest/.ssh

exec  /usr/sbin/sshd -e -D -f /pgbackrest/sshd_config