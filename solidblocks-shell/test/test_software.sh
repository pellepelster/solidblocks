#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
TEMP_DIR="${DIR}/.tmp"

source "${DIR}/../test.sh"
source "${DIR}/../software.sh"

rm -rf "${DIR}/../.bin"
rm -rf "${DIR}/../.cache"

software_ensure_consul
software_ensure_terraform
software_ensure_shellcheck
software_ensure_hugo

software_set_export_path

test_assert_matches "hugo" "hugo v0.101.0-466fa43c16709b4483689930a4f9ac8add5c9f66 linux/amd64 BuildDate=2022-06-16T07:09:16Z VendorInfo=gohugoio" "$(hugo version)"
test_assert_matches "shellcheck" "version: 0.8.0" "$(shellcheck --version | grep "version:")"
test_assert_matches "terraform" "Terraform v1.2.6" "$(terraform version | grep "Terraform v")"
test_assert_matches "consul" "Consul v1.12.3" "$(consul version | grep "Consul v")"

test_assert_file_exists "${DIR}/../.bin/shellcheck-v0.8.0/shellcheck"
test_assert_file_exists "${DIR}/../.cache/shellcheck-v0.8.0.linux.x86_64.tar.xz"


