#######################################
# hetzner-api.sh                      #
#######################################

HETZNER_CLOUD_API_URL=${HETZNER_CLOUD_API_URL:-https://api.hetzner.cloud}
HETZNER_PROVIDER_CONFIG="${SOLIDBLOCKS_DIR}/providers/hetzner.json"


function hetzner_config() {
    if [[ ! -f ${HETZNER_PROVIDER_CONFIG} ]]; then
      mkdir -p "${SOLIDBLOCKS_DIR}/providers"
      curl_wrapper -H "X-Vault-Token: ${VAULT_TOKEN}" -X GET "${VAULT_ADDR}/v1/${CLOUD_NAME}-kv/data/solidblocks/providers/hetzner" | jq '.data.data' > "${HETZNER_PROVIDER_CONFIG}"
    fi

    local path=${1:-}
    jq -r "${path}" "${HETZNER_PROVIDER_CONFIG}"
}

function hetzner_api_call() {
    local HETZNER_CLOUD_API_TOKEN="${HETZNER_CLOUD_API_TOKEN:-$(hetzner_config '.hetzner_cloud_api_token')}"
    curl_wrapper_nofail -H "Content-Type: application/json" -H "Authorization: Bearer ${HETZNER_CLOUD_API_TOKEN}" "$@"
}

function hetzner_controller_public_ips {
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=role=controller" | jq -cr '.servers | map(.public_net.ipv4.ip)'
}

function hetzner_controller_private_ips {
    local result=""

    while [[ "$(echo "${result}" | jq -r '.servers | length')" != "${CONTROLLER_NODE_COUNT}" ]]; do
        result="$(hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=role=controller")"
    done

    echo "${result}" | jq -cr '.servers | map(.private_net[0].ip)'
}

function hetzner_get_private_ip {
    local name=${1:-}
    local result=""

    while [[ ! ${result} =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; do
        result="$(hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?name=${name}" | jq -r '.servers[0].private_net[0].ip')"
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
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/floating_ips?label_selector=role=${role}" | jq -cr '.floating_ips  | map(.ip)'
}

function hetzner_floating_ip_for_server {
    local name=${1:-}
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/floating_ips?label_selector=name=${name}" | jq -cr '.floating_ips[0].ip'
}

function hetzner_get_own_floating_ip {
    hetzner_floating_ip_for_server "$(hostname)"
}
