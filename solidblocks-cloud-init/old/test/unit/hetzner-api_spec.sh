#!/usr/bin/env bash

set -eu

function config {
    echo "not implemented"
}

source ../../../lib/shell/test.sh
source ./hcloud-api-mock.sh

hcloud_api_mock_start

# overwrite hostname to force correct hostnames for the hetzner mock api calls
function hostname() {
    echo "worker-0"
}

source ../../lib/curl.sh
source ../../lib/hetzner-api.sh

assert_equals '["12.0.10.1","12.0.10.2","12.0.10.3"]' "$(hetzner_controller_public_ips)" "hetzner_controller_public_ips"
assert_equals "12.0.20.1" "$(hetzner_get_public_ip 'worker-0')" "hetzner_get_public_ip"
assert_equals "12.0.20.1" "$(hetzner_get_own_public_ip)" "hetzner_get_own_public_ip"
assert_equals "worker" "$(hetzner_get_own_role)" "hetzner_get_own_role"
assert_equals '["4.4.4.1","4.4.4.2","4.4.4.3"]' "$(hetzner_floating_ips_for_role 'controller')" "hetzner_floating_ips_for_role"
#assert_equals '4.4.4.1  controller-0  controller-0.domain
#4.4.4.2  controller-1  controller-1.domain
#4.4.4.3  controller-2  controller-2.domain' "$(hetzner_controller_hosts)" "hetzner_controller_hosts"

hcloud_api_mock_stop