#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/software.sh"
source "${DIR}/utils.sh"

software_ensure_terraform "0.12.23" "78fd53c0fffd657ee0ab5decac604b0dea2e6c0d4199a9f27db53f081d831a45"
software_set_export_path

test_assert_matches "terraform version" "Terraform v0.12.23" "$(terraform version)"

