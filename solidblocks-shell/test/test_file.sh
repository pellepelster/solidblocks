#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
TEMP_DIR="${DIR}/.tmp"

source "${DIR}/../test.sh"
source "${DIR}/../file.sh"

mkdir "${TEMP_DIR}/file_extract_to_directory_$$"
(
  cd "${TEMP_DIR}/file_extract_to_directory_$$"
  touch nomad
  zip file.zip nomad
)


# extract file to folder
file_extract_to_directory "${TEMP_DIR}/file_extract_to_directory_$$/file.zip" "${TEMP_DIR}/file_extract_to_directory_$$"
test_assert_file_exists "${TEMP_DIR}/file_extract_to_directory_$$/nomad"
test_assert_file_exists "${TEMP_DIR}/file_extract_to_directory_$$/.file.zip.extracted"

# ensure it is not extracted again when marker file is still present
rm -f "${TEMP_DIR}/file_extract_to_directory_$$/nomad"
file_extract_to_directory "${TEMP_DIR}/file_extract_to_directory_$$/file.zip" "${TEMP_DIR}/file_extract_to_directory_$$"

test_assert_file_not_exists "${TEMP_DIR}/file_extract_to_directory_$$/nomad"
test_assert_file_exists "${TEMP_DIR}/file_extract_to_directory_$$/.file.zip.extracted"

