#!/usr/bin/env bash

set -eu

source ../../../lib/shell/test.sh
source ./hcloud-api-mock.sh

hcloud_api_mock_start

SOLIDBLOCKS_DIR="/tmp/$(uuidgen)"
SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
mkdir -p "${SOLIDBLOCKS_DIR}"
cp "../test.json" "${SOLIDBLOCKS_CONFIG_FILE}"
SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}"

# overwrite hostname to force correct hostnames for the hetzner mock api calls
function hostname() {
    echo "controller-0"
}

STORAGE_LOCAL_DIR="/tmp/test.$$"

source ../../lib/curl.sh
source ../../lib/hetzner-api.sh
source ../../lib/config.sh
source ../../lib/consul.sh

CONSUL_SERVER_CONFIG="$(consul_server_config)"
assert_json "integration-test" ".datacenter" "${CONSUL_SERVER_CONFIG}" "consul_server_config"
assert_json "4.4.4.1" ".advertise_addr" "${CONSUL_SERVER_CONFIG}" "consul_server_config"
assert_json "4.4.4.1" ".client_addr" "${CONSUL_SERVER_CONFIG}" "consul_server_config"
assert_json "4.4.4.1" ".client_addr" "${CONSUL_SERVER_CONFIG}" "consul_server_config"
assert_json '["4.4.4.1","4.4.4.2","4.4.4.3"]' ".retry_join" "${CONSUL_SERVER_CONFIG}" "consul_server_config"
assert_json 'WNJLRLHVSrAE7fDqozqd6w==' ".encrypt" "${CONSUL_SERVER_CONFIG}" "consul_server_config"
assert_json '240b50bf-60ba-4d76-a70d-465159566ca2' ".acl.tokens.master" "${CONSUL_SERVER_CONFIG}" "consul_server_config"

hcloud_api_mock_stop
