#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

source "${DIR}/../../lib/terraform.sh"
source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/software.sh"

TEST_NAME="test-$(uuidgen)"

terraform_aws_state_backend "${TEST_NAME}"

software_ensure_terraform
software_set_export_path

terraform -version

(
  cd "${DIR}/terraform"
  rm -rf "${DIR}/terraform/.terraform"

  terraform init -reconfigure -backend-config="bucket=${TEST_NAME}" -backend-config="dynamodb_table=${TEST_NAME}"
  terraform apply -auto-approve
  terraform destroy -auto-approve
)

function delete_test_buckets() {
    local buckets=($(aws s3api list-buckets --query 'Buckets[?starts_with(Name, `test-`) == `true`].Name' --output text))
    for b in ${buckets[@]}
    do
      aws s3 rb --force s3://${b}
    done
}

function delete_test_tables() {
    local tables=($(aws dynamodb list-tables --query 'TableNames[?starts_with(@, `test-`) == `true`]' --output text ))
    for b in ${tables[@]}
    do
      aws dynamodb delete-table --no-cli-pager --table-name ${b}
    done
}

delete_test_buckets
delete_test_tables

