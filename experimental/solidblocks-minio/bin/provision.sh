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

export PATH="/minio/bin/:${PATH}"
export ALIAS=local
export MC_HOST_local="https://${MINIO_ADMIN_USER}:${MINIO_ADMIN_PASSWORD}@localhost"

function mc_wrapper() {
 mc --insecure $@
}

function minio_bucket_policy() {
    local bucket=${1:-}
cat <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
            "s3:*"
        ],
      "Effect": "Allow",
      "Resource": [
        "arn:aws:s3:::${bucket}/*", "arn:aws:s3:::${bucket}"
      ],
      "Sid": "${bucket}_bucket_access"
    }
  ]
}
EOF
}

function minio_ensure_bucket() {
  export bucket="${1}"
  export user_access_key="${2}"
  export user_secret_key="${3}"

  mc_wrapper admin user add "${ALIAS}" "${user_access_key}" "${user_secret_key}"

  log "creating bucket '${bucket}'"

  if ! mc_wrapper ls "${ALIAS}/${bucket}"; then
    mc_wrapper mb "${ALIAS}/${bucket}"
  fi

  minio_bucket_policy "${bucket}" > "/tmp/${user_access_key}_policy.$$"
  mc_wrapper admin policy add "${ALIAS}" "${bucket}_bucket_access" "/tmp/${user_access_key}_policy.$$"
  mc_wrapper admin policy set "${ALIAS}" "${bucket}_bucket_access" "user=${user_access_key}"
}

function minio_ensure_buckets() {
  if [[ -z $* ]]; then
    log "no bucket configurations provided, no buckets will be created"
  else
    local bucket_specs=($(echo "$*" | tr '#' "\n"))

    for bucket_spec in "${bucket_specs[@]}"
    do
        local spec_parts=($(echo "${bucket_spec}" | tr ':' "\n"))
        local bucket="${spec_parts[0]:-}"
        local access_key="${spec_parts[1]:-}"
        local secret_key="${spec_parts[2]:-}"

        if [[ -n "${bucket}" ]] && [[ -n "${access_key}" ]] && [[ -n "${secret_key}" ]]; then
          minio_ensure_bucket "${bucket}" "${access_key}" "${secret_key}"
        else
          echo "invalid bucket spec '${bucket_spec}'"
        fi
    done

  fi
}

function wait_for_minio() {
  MINIO_HOST="localhost"
  MINIO_PORT="443"

  log "waiting for minio at '${MINIO_HOST}:${MINIO_PORT}'"
  while ! nc -z "${MINIO_HOST}" "${MINIO_PORT}"; do
    sleep 1
    log "still waiting for minio at '${MINIO_HOST}:${MINIO_PORT}'"
  done
  log "minio is started at '${MINIO_HOST}:${MINIO_PORT}'"
}

wait_for_minio

minio_ensure_buckets "$*"

log "provisioning completed"