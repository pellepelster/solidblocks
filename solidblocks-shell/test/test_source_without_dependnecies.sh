#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
TEMP_DIR="${DIR}/.tmp"

source "${DIR}/../test.sh"
source "${DIR}/../hashicorp.sh"

hashicorp_ensure_terraform "${TEMP_DIR}/hashicorp_ensure_terraform_$$" "0.12.23" "78fd53c0fffd657ee0ab5decac604b0dea2e6c0d4199a9f27db53f081d831a45"
TERRAFORM_VERSION=$("${TEMP_DIR}/hashicorp_ensure_terraform_$$/terraform" -version)
test_assert_matches "terraform version" "Terraform v0.12.23" "${TERRAFORM_VERSION}"

