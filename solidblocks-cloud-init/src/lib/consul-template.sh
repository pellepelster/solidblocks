#######################################
# consul-template.sh                  #
#######################################

CONSUL_TEMPLATE_VERSION="0.27.2"
CONSUL_TEMPLATE_CHECKSUM="d3d428ede8cb6e486d74b74deb9a7cdba6a6de293f3311f178cc147f1d1837e8"
CONSUL_TEMPLATE_URL="https://releases.hashicorp.com/consul-template/${CONSUL_TEMPLATE_VERSION}/consul-template_${CONSUL_TEMPLATE_VERSION}_linux_amd64.zip"

function consul_template_install() {
    local target_file="$(mktemp)"
    download_and_verify_checksum "${CONSUL_TEMPLATE_URL}" "${target_file}" "${CONSUL_TEMPLATE_CHECKSUM}"
    unzip -o -d /usr/local/bin "${target_file}"
    rm -rf "${target_file}"
}

