#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
TEMP_DIR="${DIR}/.tmp"

source "${DIR}/../test.sh"
source "${DIR}/../software.sh"

rm -rf "${DIR}/../.bin"
rm -rf "${DIR}/../.cache"

software_ensure_consul "1.8.4" "0d74525ee101254f1cca436356e8aee51247d460b56fc2b4f7faef8a6853141f"
software_ensure_terraform "0.13.4" "a92df4a151d390144040de5d18351301e597d3fae3679a814ea57554f6aa9b24"

software_ensure_shellcheck
software_ensure_hugo

software_ensure_export_path

test_assert_matches "hugo" "hugo v0.101.0-466fa43c16709b4483689930a4f9ac8add5c9f66 linux/amd64 BuildDate=2022-06-16T07:09:16Z VendorInfo=gohugoio" "$(hugo version)"
test_assert_matches "shellcheck" "version: 0.8.0" "$(shellcheck --version | grep "version:")"
test_assert_matches "terraform" "Terraform v0.13.4" "$(terraform version | grep "Terraform v")"
test_assert_matches "consul" "Consul v1.8.4" "$(consul version | grep "Consul v")"



