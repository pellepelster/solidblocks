#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
TEMP_DIR="${DIR}/.tmp"

source "${DIR}/../test.sh"
source "${DIR}/../download.sh"
source "${DIR}/../hashicorp.sh"

# downloaded file
download_and_verify_checksum "https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip" "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
test_assert_matches "downloaded file checksum" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253" "$(sha256sum ${TEMP_DIR}/download_and_verify_checksum_$$/file.zip | cut -d' ' -f1)"

# invalidate file
echo "XXX" > "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip"

# ensure it gets re-downloaded
download_and_verify_checksum "https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip" "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
test_assert_matches "downloaded file checksum" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253" "$(sha256sum ${TEMP_DIR}/download_and_verify_checksum_$$/file.zip | cut -d' ' -f1)"

# extract file to folder
file_extract_to_directory "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "${TEMP_DIR}/download_and_verify_checksum_$$"
test_assert_file_exists "${TEMP_DIR}/download_and_verify_checksum_$$/nomad"
test_assert_file_exists "${TEMP_DIR}/download_and_verify_checksum_$$/.file.zip.extracted"

# ensure it is not extracted again when marker file is still present
rm -f "${TEMP_DIR}/download_and_verify_checksum_$$/nomad"
file_extract_to_directory "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "${TEMP_DIR}/file_extract_to_directory_$$"

test_assert_file_not_exists "${TEMP_DIR}/download_and_verify_checksum_$$/nomad"
test_assert_file_exists "${TEMP_DIR}/download_and_verify_checksum_$$/.file.zip.extracted"

hashicorp_ensure "terraform" "0.13.4" "a92df4a151d390144040de5d18351301e597d3fae3679a814ea57554f6aa9b24" "${TEMP_DIR}/hashicorp_ensure_$$"
test_assert_file_exists "${TEMP_DIR}/hashicorp_ensure_$$/terraform"

hashicorp_ensure_terraform "${TEMP_DIR}/hashicorp_ensure_terraform_$$"
TERRAFORM_VERSION=$("${TEMP_DIR}/hashicorp_ensure_terraform_$$/terraform" -version)
test_assert_matches "terraform version" "Terraform v0.13.4" "${TERRAFORM_VERSION}"

hashicorp_ensure_terraform "${TEMP_DIR}/hashicorp_ensure_terraform_$$"
TERRAFORM_VERSION=$("${TEMP_DIR}/hashicorp_ensure_terraform_$$/terraform" -version)
test_assert_matches "terraform version" "Terraform v0.13.4" "${TERRAFORM_VERSION}"

hashicorp_ensure_terraform "${TEMP_DIR}/hashicorp_ensure_terraform_$$" "0.12.23" "78fd53c0fffd657ee0ab5decac604b0dea2e6c0d4199a9f27db53f081d831a45"
TERRAFORM_VERSION=$("${TEMP_DIR}/hashicorp_ensure_terraform_$$/terraform" -version)
test_assert_matches "terraform version" "Terraform v0.12.23" "${TERRAFORM_VERSION}"

hashicorp_ensure_consul "${TEMP_DIR}/hashicorp_ensure_terraform_$$"
CONSUL_VERSION=$("${TEMP_DIR}/hashicorp_ensure_terraform_$$/consul" -version)
test_assert_matches "terraform version" "Consul v1.8.4" "${CONSUL_VERSION}"
