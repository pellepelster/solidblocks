#######################################
# consul-template.sh                  #
#######################################

CONSUL_TEMPLATE_VERSION="0.19.5"
CONSUL_TEMPLATE_CHECKSUM="e6b376701708b901b0548490e296739aedd1c19423c386eb0b01cfad152162af"
CONSUL_TEMPLATE_URL="https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_linux_amd64.zip"

function consul_template_install() {
    local target_file="$(mktemp)"
    download_and_verify_checksum "${CONSUL_TEMPLATE_URL}" "${target_file}" "${CONSUL_TEMPLATE_CHECKSUM}"
    unzip -o -d /usr/local/bin "${target_file}"
    rm -rf "${target_file}"
}

