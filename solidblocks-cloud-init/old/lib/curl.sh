function curl_wrapper() {
    while ! curl --retry 25 --retry-connrefused --fail --silent --location --show-error "$@"; do
        sleep 5
    done
}

function curl_wrapper_nofail() {
    while ! curl --retry 25 --retry-connrefused --silent --location --show-error "$@"; do
        sleep 5
    done
}