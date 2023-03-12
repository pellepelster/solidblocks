#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"
source "${_DIR}/aws.sh"

# see https://pellepelster.github.io/solidblocks/shell/terraform/#terraform_aws_state_backend
function terraform_aws_state_backend() {
  local name="${1:-}"
  shift || true

  aws_bucket_ensure "${name}"
  aws_dynamodb_ensure "${name}" "AttributeName=LockID,AttributeType=S" "AttributeName=LockID,KeyType=HASH"
}

