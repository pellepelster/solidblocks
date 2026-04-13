_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

CURL_WRAPPER_RETRY_DELAY=${CURL_WRAPPER_RETRY_DELAY:-5}
CURL_WRAPPER_RETRIES=${CURL_WRAPPER_RETRIES:-10}

function curl_wrapper() {
    local try=0
    while [ $try -lt ${CURL_WRAPPER_RETRIES} ] && ! curl --retry-connrefused --fail --silent --location --show-error "$@"; do
        try=$((try+1))
        echo "curl call '$@' (${try}/${CURL_WRAPPER_RETRIES}) failed, retrying in ${CURL_WRAPPER_RETRY_DELAY} seconds" 1>&2;
        sleep "${CURL_WRAPPER_RETRY_DELAY}"
    done
}
