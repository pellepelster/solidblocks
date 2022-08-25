#!/usr/bin/env bash

set -eu

function log() {
  echo "[solidblocks-minio] $*"
}

if [[ -z "${MINIO_ADMIN_USER:-}" ]]; then
  log "MINIO_ADMIN_USER not set"
  exit 1
fi

if [[ -z "${MINIO_ADMIN_PASSWORD:-}" ]]; then
  log "MINIO_ADMIN_PASSWORD not set"
  exit 1
fi

if [[ -z "${MINIO_TLS_PRIVATE_KEY:-}" ]]; then
  log "MINIO_TLS_PRIVATE_KEY not set"
  exit 1
fi

if [[ -z "${MINIO_TLS_PUBLIC_KEY:-}" ]]; then
  log "MINIO_TLS_PUBLIC_KEY not set"
  exit 1
fi

if ! mount | grep "${DATA_DIR}"; then
    log "storage dir '${DATA_DIR}' not mounted"
    exit 1
fi

export MINIO_ROOT_USER="${MINIO_ADMIN_USER}"
export MINIO_ROOT_PASSWORD="${MINIO_ADMIN_PASSWORD}"

MINIO_OPTS="--console-address :9001"

mkdir -p /minio/certificates
echo -n "${MINIO_TLS_PRIVATE_KEY}" | base64 -d > /minio/certificates/private.key
echo -n "${MINIO_TLS_PUBLIC_KEY}" | base64 -d > /minio/certificates/public.crt

MINIO_OPTS="${MINIO_OPTS} --certs-dir /minio/certificates --address :${MINIO_HTTPS_PORT}"

/minio/bin/provision.sh "${BUCKET_SPECS:-}" &

exec /minio/bin/minio server ${MINIO_OPTS} "${DATA_DIR}"