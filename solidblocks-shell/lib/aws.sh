#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"

function aws_dynamodb_table_exists() {
  local table_name="${1:-}"
  shift || true

  local status="$(aws dynamodb describe-table --table-name "${table_name}" --output json 2>&1)"

  if echo "${status}" | grep --silent 'ResourceNotFoundException'; then
    echo "table_not_found"
  elif echo "${status}" | grep --silent 'An error occurred'; then
    echo "error(${status})"
  elif echo "${status}" | grep --silent "${table_name}"; then
    echo "table_exists"
  else
    echo "error(${status})"
  fi
}

function aws_dynamodb_table_status() {
  local table_name="${1:-}"

  aws dynamodb describe-table \
      --table-name "${table_name}" \
      --output json | jq -r '.Table.TableStatus'
}

# see https://pellepelster.github.io/solidblocks/shell/aws/#aws_dynamodb_ensure
function aws_dynamodb_ensure() {
  local table_name="${1:-}"
  local attribute_definitions="${2:-}"
  local key_schema="${3:-}"
  local throughput="${4:-ReadCapacityUnits=1,WriteCapacityUnits=1}"

  local status="$(aws_dynamodb_table_exists "${table_name}")"

  if [[ "${status}" =~ error(.*) ]]; then
    log_echo_die "failed to query table status for table '${table_name}' (${status})"
  fi

  if [[ "${status}" == "table_not_found" ]]; then
    log_echo "creating table '${table_name}'"

    aws dynamodb create-table \
      --table-name "${table_name}" \
      --output json \
      --attribute-definitions ${attribute_definitions} \
      --key-schema ${key_schema} \
      --provisioned-throughput ${throughput} 2>&1 > /dev/null

    local table_status="$(aws_dynamodb_table_status "${table_name}")"
    while [[ "${table_status}" != "ACTIVE" ]]; do
      sleep 1
      log_echo "waiting for table '${table_name}' to become ready, current status is '${table_status}'"
      table_status="$(aws_dynamodb_table_status "${table_name}")"
    done

    log_echo "created table '${table_name}'"
  else
    log_echo "table '${table_name}' already exists"
  fi
}

function aws_dynamodb_delete() {
  local table_name="${1:-}"

  local status="$(aws_dynamodb_table_exists "${table_name}")"

  if [[ "${status}" == "table_exists" ]]; then
    log_echo "deleting table '${table_name}'"

    aws dynamodb delete-table \
      --output json \
      --table-name "${table_name}" 2>&1 > /dev/null

    while [[ "$(aws_dynamodb_table_exists "${table_name}")" != "table_not_found" ]]; do
      sleep 1
      log_echo "waiting for table '${table_name}' to be deleted"
    done

  else
    log_echo "table '${table_name}' does not exist"
  fi
}

function aws_bucket_exists() {
  local bucket_name="${1:-}"
  shift || true

  if aws s3api head-bucket --bucket "${bucket_name}" --output json 2> /dev/null; then
    echo "bucket_exists"
  else
    local status="$(aws s3api head-bucket --bucket "${bucket_name}" --output json 2>&1)"

    if echo "${status}" | grep --silent '404'; then
      echo "bucket_not_found"
    elif echo "${status}" | grep --silent 'An error occurred'; then
      echo "error(${status})"
    else
      echo "error(${status})"
    fi
  fi
}

# see https://pellepelster.github.io/solidblocks/shell/aws/#aws_bucket_ensure
function aws_bucket_ensure() {
  local bucket_name="${1:-}"
  local region="${2:-eu-central-1}"

  local status="$(aws_bucket_exists "${bucket_name}")"

  if [[ "${status}" =~ error(.*) ]]; then
    log_echo_die "failed to query bucket status for bucket '${bucket_name}' (${status})"
  fi

  if [[ "${status}" == "bucket_not_found" ]]; then
    log_echo "creating bucket '${bucket_name}'"
    local create_result=$(aws s3api create-bucket \
          --region "${region}" \
          --output json \
          --acl private \
          --bucket "${bucket_name}" \
          --create-bucket-configuration LocationConstraint=${region})

    local location="$(echo "${create_result}" | jq -r '.Location')"
    log_echo "created bucket '${bucket_name}' (${location})"

    while [[ "$(aws_bucket_exists "${bucket_name}")" != "bucket_exists" ]]; do
      sleep 1
      log_echo "waiting for bucket '${bucket_name}' creation to finish"
    done

  else
    log_echo "bucket '${bucket_name}' already exists"
  fi
}

function aws_bucket_delete() {
  local bucket_name="${1:-}"
  shift || true
  local location="eu-west-1"

  if [[ "$(aws_bucket_exists "${bucket_name}")" == "bucket_exists" ]]; then
    aws s3api delete-bucket \
          --region ${location} \
          --output json \
          --bucket "${bucket_name}"
  fi
}
