#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/download.sh"
source "${DIR}/utils.sh"

# downloaded file
download_and_verify_checksum "https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip" "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
test_assert_matches "downloaded file checksum" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253" "$(sha256sum ${TEMP_DIR}/download_and_verify_checksum_$$/file.zip | cut -d' ' -f1)"

# invalidate file
echo "XXX" > "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip"

# ensure it gets re-downloaded
download_and_verify_checksum "https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip" "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
test_assert_matches "downloaded file checksum" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253" "$(sha256sum ${TEMP_DIR}/download_and_verify_checksum_$$/file.zip | cut -d' ' -f1)"

