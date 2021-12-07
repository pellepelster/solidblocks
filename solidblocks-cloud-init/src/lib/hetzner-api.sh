#######################################
# hetzner-api.sh                      #
#######################################

HETZNER_CLOUD_API_URL=${HETZNER_CLOUD_API_URL:-https://api.hetzner.cloud}


function hetzner_api_call() {
    curl_wrapper_nofail -H "Content-Type: application/json" -H "Authorization: Bearer ${HETZNER_CLOUD_API_TOKEN_RO_KEY}" "$@"
}

function hetzner_controller_public_ips {
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=solidblocks/role=controller" | jq -cr '.servers | map(.public_net.ipv4.ip)'
}

function hetzner_controller_private_ips {
    local result=""

    while [[ "$(echo "${result}" | jq -r '.servers | length')" != "${CONTROLLER_NODE_COUNT}" ]]; do
        result="$(hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=solidblocks/role=controller")"
        sleep 10
    done

    echo "${result}" | jq -cr '.servers | map(.private_net[0].ip)'
}

function hetzner_get_private_ip {
    local name=${1:-}
    local result=""

    while [[ ! ${result} =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; do
        result="$(hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?name=${name}" | jq -r '.servers[0].private_net[0].ip')"
        sleep 10
    done

    echo "${result}"
}

function hetzner_get_own_private_ip {
    hetzner_get_private_ip "$(hostname)"
}

function hetzner_get_public_ip {
    local name=${1:-}
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?name=${name}" | jq -r '.servers[0].public_net.ipv4.ip'
}

function hetzner_get_own_public_ip {
    hetzner_get_public_ip "$(hostname)"
}

function hetzner_get_own_role {
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?name=$(hostname)" | jq -r '.servers[0].labels.role'
}

function hetzner_floating_ips_for_role {
    local role=${1:-}
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/floating_ips?label_selector=solidblocks/role=${role}" | jq -cr '.floating_ips  | map(.ip)'
}

function hetzner_floating_ip_for_server {
    local name=${1:-}
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/floating_ips?label_selector=name=${name}" | jq -cr '.floating_ips[0].ip'
}

function hetzner_get_own_floating_ip {
    hetzner_floating_ip_for_server "$(hostname)"
}
