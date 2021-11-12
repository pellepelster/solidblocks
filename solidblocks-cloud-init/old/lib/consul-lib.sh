#######################################
# consul-lib.sh                       #
#######################################

function consul_kv_put() {
    local path=${1:-}
    local value=${2:-}

    while [[ ! $(CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN:-$(config .consul_master_token)} consul kv put "${path}" "${value}") ]]
    do
        echo "error writing to '${path}' waiting..."
        sleep 5
    done
}

function consul_kv_get() {
    local path=${1:-}
    CONSUL_HTTP_TOKEN=${CONSUL_HTTP_TOKEN:-$(config .consul_master_token)} consul kv get "${path}"
}

function wait_for_consul_key {
    local key="${1:-}"
    local retries="${2:-999999}"
    local wait="${3:-5}"

    local retry=0
    while ! consul_kv_get "${key}" && [[ ${retry} -lt ${retries} ]]
    do
        echo "consul key '${key}' not found, waiting ${wait} seconds, retry ${retry} of ${retries}"
        sleep "${wait}"
        retry=$((retry+1))
    done
}