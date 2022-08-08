#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"

CURL_WRAPPER_RETRY_DELAY=${CURL_WRAPPER_RETRY_DELAY:-5}
CURL_WRAPPER_RETRIES=${CURL_WRAPPER_RETRIES:-10}

function curl_wrapper() {
    local try=0

    while [ $try -lt ${CURL_WRAPPER_RETRIES} ] && ! curl --retry-connrefused --fail --silent --location --show-error "$@"; do
        try=$((try+1))
        log_echo_err "curl call '$@' (${try}/${CURL_WRAPPER_RETRIES}) failed, retrying in ${CURL_WRAPPER_RETRY_DELAY} seconds"
        sleep "${CURL_WRAPPER_RETRY_DELAY}"
    done
}
