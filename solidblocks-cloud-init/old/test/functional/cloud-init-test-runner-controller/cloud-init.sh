#!/usr/bin/env bash

set -x

#######################################
# ENVIRONMENT VARIABLES               #
#######################################


export SOLIDBLOCKS_CONFIG_FILE
SOLIDBLOCKS_CONFIG_FILE=${SOLIDBLOCKS_CONFIG_FILE:-/solidblocks/solidblocks.json}

export SOLIDBLOCKS_CONFIG_DIR
SOLIDBLOCKS_CONFIG_DIR="$(dirname "${SOLIDBLOCKS_CONFIG_FILE}")"

export SOLIDBLOCKS_CERTIFICATES_DIR
SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_CONFIG_DIR}/certificates"

export SOLIDBLOCKS_GROUP
SOLIDBLOCKS_GROUP="solidblocks"

DEBUG_LEVEL="${DEBUG_LEVEL:-0}"
DISABLE_MANAGEMENT="${DISABLE_MANAGEMENT:-0}"
#######################################
# solidctl.sh                         #
#######################################

function ensure_config_dir() {
    if [[ ! -d ${SOLIDBLOCKS_CONFIG_DIR} ]]; then
        mkdir -p "${SOLIDBLOCKS_CONFIG_DIR}"
        groupadd "${SOLIDBLOCKS_GROUP}"
        chmod 750 "${SOLIDBLOCKS_CONFIG_DIR}"
        chgrp "${SOLIDBLOCKS_GROUP}" "${SOLIDBLOCKS_CONFIG_DIR}"

        mkdir -p "${SOLIDBLOCKS_CERTIFICATES_DIR}"
        chmod 750 "${SOLIDBLOCKS_CERTIFICATES_DIR}"
        chgrp "${SOLIDBLOCKS_GROUP}" "${SOLIDBLOCKS_CERTIFICATES_DIR}"
    fi
}

ensure_config_dir

cat <<-EOF >"${SOLIDBLOCKS_CONFIG_FILE}"
{
  "cloud_name": "integration-test",
  "hetzner_cloud_api_token": "YKKZ2PdUbnUZvlaLAHXM5f0XXJqtqzP5uaIrQlwgVNPkD3GEruB93O2Vs3QhCdol",
  "domain": "integration-test.blcks.de",
  "consul_secret": "WNJLRLHVSrAE7fDqozqd6w==",
  "consul_master_token": "240b50bf-60ba-4d76-a70d-465159566ca2",
  "registry_username": "pellepelster",
  "registry_password": "sbCrYDgkks9jpeEE1-vA",
  "root_username": "admin",
  "root_password": "fec57198-f823-49a6-ad6d-67b15b7754f7",
  "ca_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDAebqe0B4Y0+ey\nKC82DrLxWBm2uuLupyUzlLLOquYIvMQwI1FEvIA+q/Ak8iFu+QW1ImUWYYiBBi2L\nyuUVesu0QNPFg+z6noNFRuS68f0AqOQnt54LKnU2npvLHwxXbICM2lrd+KVKJ/l/\nJNt3uBgKbHm0xFY2BHatfLJHtnGEL/hFfiax90HCTl/N9LdDz4KW61/3Cij4wBOt\nnV/LEZeTgGkQ9oibKiv4/ojlfHvpISxkeao4v3gUUx8cM9Ye8gLqCZTd5thRuVlf\ntkzdA/6iwRnjgESqstEUQTuPhO4EDI1FaOqKd5E+sbLjlC627ffiJrjgh/k25Fy7\nU7lP0BNhAgMBAAECggEAAjb0WthIsbRF0yLgu4XARoQjFEXtZLMXe72J2tRpvgR+\nr7zG9jGYD9XhIb+yevPSaRLlUwmvbSf3IAe8io/QO7mk5ih+MnpenCO0b+KRwhnv\nZCCZ/jAQdgmC/+Y9JAgLfki96RYaasfFIxLMvqwnhKkvs6YwWsHtjqdOaCz/0eDz\n8qSjNL5MWXghWjFKFS4G+3T+UbFHX4bBHh0KossBfmUVgzrI2fD1lNdybo4xonKY\nome9vIelkEX1M113IliP1bZO/c2okzU4uu0d8A1RbJPEyolP6GnXh0OdFubjaDTl\n08r/2xxJ8xWMgXbTML4TOA9jEP5OduV+0OsY+d7+5QKBgQD45kwEf4MAFfc8ItQX\nkWqVAX2Peg3aeUuC2wCQYr0ET6Ob7p8O59qzU+IKU6fCkfZ+9T2rOgQyTBwIDdRZ\nDJP9CdRdWm7zIRoA7JQKhuknJpBJHd9W7GpGpEXISvMetu8Hw3u/FQWFBrS8qUgA\nBbYF0YJ2UYzf5ylZSwVv+gBxlQKBgQDF916LTuJxyg1cpiDl7JQeOGPY6YftBMXo\niiKRSAf7Ci4jrmQ8NIx8aymytUOzCk7jwGndAthSy2ed8spBwRQ1XgX1llDBG8Gu\nzGnoirHdNQDLdoWXPU6zFUNizCxP4JMc/prEDFqhlCFtCwZzW8a2TExnUfHrNsrB\ncCpWPwv/nQKBgQDDGWoe4ICkEZPBjJ9pde/wqsFsNMUlKozZHqebhfoZpf9eHDaL\nrYwNo0P8ykQmzNlF/SAA1rIxZt1WJtj7kFN0Nj9Dib6MT7cdYFROyB98M8tHtOW7\nMgfAJcYZUT6vJb/J+x5F0smH05DajWrFsbRTbT1xV00wuPb9akPx2Sp93QKBgDiM\n1kwTJ6p7nl+F8UXc097iGtuesj5pq6MmuoMnwWfb25xDt8xe8nakIhAzqXgejLSQ\nhW0l3+eexSWgclhgMEaiai9iVgUjyasGJ4bO/8oB1w1H+Rdf4vhTLaGuU0YqD7wr\nmshAAy++4eGFUb1oTRZMK1MkXGflifvB78YdUm8dAoGBAN4fj6D/Y/1X0zz/Vktg\nAwCkguEOz1T4wa4huWWt1fJP8mwUSpzhOvg1w+uYS7nYg6CDD4FgZaaDRHsbO3u3\nP7RzjT2FOnkJDTIZupvKOb1RpVmmeqKOgSB5hDpEPnHqjS2xC0CuufAmReoFNFao\nhbJEghwT6lV8DGjmLdGnkHYT\n-----END PRIVATE KEY-----\n",
  "ca_certificate": "-----BEGIN CERTIFICATE-----\nMIICwDCCAaigAwIBAgIE4iwchzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZy\nb290Q0EwHhcNMTkwNTMwMjEwMDU1WhcNMjAwNTMwMjEwMDU1WjARMQ8wDQYDVQQD\nDAZyb290Q0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDAebqe0B4Y\n0+eyKC82DrLxWBm2uuLupyUzlLLOquYIvMQwI1FEvIA+q/Ak8iFu+QW1ImUWYYiB\nBi2LyuUVesu0QNPFg+z6noNFRuS68f0AqOQnt54LKnU2npvLHwxXbICM2lrd+KVK\nJ/l/JNt3uBgKbHm0xFY2BHatfLJHtnGEL/hFfiax90HCTl/N9LdDz4KW61/3Cij4\nwBOtnV/LEZeTgGkQ9oibKiv4/ojlfHvpISxkeao4v3gUUx8cM9Ye8gLqCZTd5thR\nuVlftkzdA/6iwRnjgESqstEUQTuPhO4EDI1FaOqKd5E+sbLjlC627ffiJrjgh/k2\n5Fy7U7lP0BNhAgMBAAGjIDAeMA4GA1UdDwEB/wQEAwICBDAMBgNVHRMEBTADAQH/\nMA0GCSqGSIb3DQEBCwUAA4IBAQCwKXYGe8yH3XKk+H9dGpe1OffOkS/O0V809RsZ\nP8U7Isj8I3fe9yVYdJkpJrICkFNPi2GtEq7cz8FSqZmZwIuSIl+1mbrWGx6Oxii5\nYhiczWlZyEsbI/J0g+dPGfoGiy8IYgeeGtknTEArI3VMBBQR31NSC2qrWLj3eXFp\ngYFUK+KSYXGE646oU4wHScrhs0eCFxwc4YE9XdB3l2COiS48n+J43hL5jry1cq7l\nobNNhOCkKWQr1NBez5d55s2sET/3rJJjJvMMrHptF1B2g/UpFytv+3WY/SrIdkA/\nxZ2n49BdZp/PF+lb/Pz27d5ULnkP6ILlq1qnyohI5sTT5+DX\n-----END CERTIFICATE-----\n"
}
EOF
chgrp "${SOLIDBLOCKS_GROUP}" "${SOLIDBLOCKS_CONFIG_FILE}"

#######################################
# common.sh                           #
#######################################

function check_and_install {
	local package=${1}
	echo -n "checking if package '${package}' is installed..."
	if [[ $(dpkg-query -W -f='${Status}' "${package}" 2>/dev/null | grep -c "ok installed") -eq 0 ]];
	then
		echo "not found, installing now"
		while ! apt-get install --no-install-recommends -qq -y "${package}"; do
    		echo "installing failed retrying in 10 seconds"
    		sleep 10
		done
	else
		echo "ok"
	fi
}

function create_directory_if_needed {
    local directory="${1}"

    if [[ ! -d "${directory}" ]]; then
        mkdir -p "${directory}"
    fi
}

function download_and_verify_checksum {
    local url=${1}
    local target_file=${2}
    local checksum=${3}

    if [[ -f "${target_file}" ]]; then
        local target_file_checksum
        target_file_checksum=$(sha256sum "${target_file}" | cut -d' ' -f1)
        if [[ "${target_file_checksum}" = "${checksum}" ]]; then
            echo "${url} already downloaded"
            return
        fi
    fi

    create_directory_if_needed "$(dirname "${target_file}")"

    echo -n "downloading ${url}..."
    curl -sL "${url}" --output "${target_file}" > /dev/null
    echo "done"


    echo -n "verifying checksum..."
    echo "${checksum}" "${target_file}" | sha256sum --check --quiet
    echo "done"
}

function wait_for_consul_key {
    local key="${1:-}"
    while [[ ! $(consul kv get "${key}") ]]
    do
        echo "waiting for consul key '${key}'"
        sleep 5
    done
}

apt-get update

check_and_install "jq"
check_and_install "curl"
check_and_install "unzip"

function ca_install_root_cert() {
    ca_ensure_certificates_dir
    local system_ca_file
    system_ca_file="/usr/local/share/ca-certificates/$(config '.cloud_name').crt"
    config '.ca_certificate' > "${system_ca_file}"
    ln -s "${system_ca_file}" "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem"
    update-ca-certificates
}

function ca_ensure_certificates_dir() {
    ensure_config_dir

    if [[ ! -d ${SOLIDBLOCKS_CERTIFICATES_DIR} ]]; then
        mkdir -p "${SOLIDBLOCKS_CERTIFICATES_DIR}"
        chmod 750 "${SOLIDBLOCKS_CERTIFICATES_DIR}"
        chgrp "${SOLIDBLOCKS_GROUP}" "${SOLIDBLOCKS_CERTIFICATES_DIR}"
    fi
}

function ca_ensure_permissions(){
    local file=${1:-}
    local permissions=${2:-}
    chmod "${permissions}" "${file}"
    chgrp "${SOLIDBLOCKS_GROUP}" "${file}"
}

function ca_ensure_file_with_permissions(){
    local file=${1:-}
    local permissions=${2:-}
    touch "${ca_certificate_file}"
    ca_ensure_permissions "${file}" "${permissions}"
}

function ca_create_certificate() {
    (
        local certificate_name="${1:-}"
        local target_dir="${2:-}"
        local ca_dir
        ca_dir="${target_dir}/ca"

        mkdir -p "${target_dir}"
        mkdir -p "${ca_dir}"

        local ca_key_file
        ca_key_file="${ca_dir}/ca.key.pem"

        local ca_certificate_file
        ca_certificate_file="${ca_dir}/ca.cert.pem"

        ca_ensure_file_with_permissions "${ca_key_file}" 600
        config '.ca_key' > "${ca_key_file}"

        ca_ensure_file_with_permissions "${ca_certificate_file}" 660
        config '.ca_certificate' > "${ca_certificate_file}"

        local certificate_key_file
        certificate_key_file="${target_dir}/${certificate_name}.key.pem"

        local certificate_csr_file
        certificate_csr_file="${target_dir}/${certificate_name}.csr.pem"

        local certificate_file
        certificate_file="${target_dir}/${certificate_name}.cert.pem"

        openssl genrsa -out "${certificate_key_file}" 4096
        ca_ensure_permissions "${certificate_key_file}" 660

        openssl req -new -key "${certificate_key_file}" -out "${certificate_csr_file}" -sha512 \
            -subj "/CN=${certificate_name}/O=Solidblocks/C=DE"
        ca_ensure_permissions "${certificate_csr_file}" 600

        openssl x509 -req -in "${certificate_csr_file}" -CA "${ca_certificate_file}" -CAkey "${ca_key_file}" -CAcreateserial -out "${certificate_file}" -days 365 -sha512
        ca_ensure_permissions "${certificate_file}" 660

        function finish {
            echo "cleaning up after certificate creation"
            rm -rf "${ca_dir}"
        }
        trap finish EXIT

    )
}
#######################################
# config.sh                           #
#######################################

function config() {
    local path=${1:-}
    jq -r "${path}" "${SOLIDBLOCKS_CONFIG_FILE}"
}

#######################################
# consul.sh                           #
#######################################

CONSUL_CHECKSUM="58fbf392965b629db0d08984ec2bd43a5cb4c7cc7ba059f2494ec37c32fdcb91"
CONSUL_URL="https://releases.hashicorp.com/consul/1.5.1/consul_1.5.1_linux_amd64.zip"

function consul_agent_config {
cat <<-EOF
{
  "datacenter": "$(config '.cloud_name')",
  "advertise_addr": "$(hetzner_get_own_public_ip)",
  "client_addr": "$(hetzner_get_own_public_ip)",
  "leave_on_terminate": true,
  "retry_join": $(hetzner_controller_public_ips),
  "ca_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem",
  "cert_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).cert.pem",
  "key_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem",
  "addresses": {
    "http": "127.0.0.1",
    "dns": "127.0.0.1"
  },
  "ports": {
     "dns": 8553
  },
  "encrypt": "$(config '.consul_secret')",
  "verify_incoming": true,
  "verify_incoming_https": false,
  "verify_outgoing": true,
  "enable_script_checks": true,
  "acl": {
    "enabled": true,
    "default_policy": "deny",
    "down_policy": "deny",
    "tokens": {
      "default": "${CONSUL_HTTP_TOKEN}"
    }
  }
}
EOF
}

function consul_server_config {
cat <<-EOF
{
  "datacenter": "$(config '.cloud_name')",
  "server": true,
  "ui": true,
  "advertise_addr": "$(hetzner_get_own_public_ip)",
  "client_addr": "$(hetzner_get_own_public_ip)",
  "leave_on_terminate": true,
  "retry_join": $(hetzner_controller_public_ips),
  "bootstrap_expect": 3,
  "ca_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem",
  "cert_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).cert.pem",
  "key_file": "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem",
  "verify_incoming": true,
  "verify_incoming_https": false,
  "verify_outgoing": true,
  "addresses": {
    "https": "0.0.0.0",
    "http": "0.0.0.0",
    "dns": "127.0.0.1"
  },
  "ports": {
    "dns": 8553,
    "http": 8500,
    "https": 8501
  },
  "encrypt": "$(config '.consul_secret')",
  "acl": {
    "enabled": true,
    "default_policy": "deny",
    "down_policy": "deny",
    "tokens": {
      "master": "$(config '.consul_master_token')",
      "agent": "$(config '.consul_master_token')",
      "default": "$(config '.consul_master_token')"
    }
  }
}
EOF
}

function consul_agent_systemd_config() {
cat <<-EOF
[Unit]
Description=consul server
Requires=network-online.target
After=network-online.target

[Service]
User=consul
EnvironmentFile=-/etc/sysconfig/consul
Restart=on-failure
ExecStart=/usr/local/bin/consul agent $CONSUL_FLAGS -config-dir=/etc/consul -data-dir=${STORAGE_LOCAL_DIR}/consul
ExecReload=/bin/kill -HUP $MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function consul_server_systemd_config() {
cat <<-EOF
[Unit]
Description=consul server
Requires=network-online.target
After=network-online.target

[Service]
User=consul
EnvironmentFile=-/etc/sysconfig/consul
Restart=on-failure
ExecStart=/usr/local/bin/consul agent -ui -server $CONSUL_FLAGS -config-dir=/etc/consul --data-dir=${STORAGE_LOCAL_DIR}/consul
ExecReload=/bin/kill -HUP $MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function consul_install() {
    local target_file="/tmp/consul.zip"
    download_and_verify_checksum "${CONSUL_URL}" "${target_file}" "${CONSUL_CHECKSUM}"
    unzip -o -d /usr/local/bin ${target_file}
}

function consul_create_user() {
    local user
    user="consul"
    if [[ ! $(id -u "${user}") ]]; then
        useradd --no-create-home --no-user-group --groups solidblocks "${user}"
    fi
}

function consul_dns_policy() {
cat <<-EOF
{
  "Name": "dns-discovery-read",
  "Description": "dns service and node discovery",
  "Rules": "node_prefix \"\" { policy = \"read\"}, service_prefix \"\" { policy = \"read\" }"
}
EOF
}

function consul_default_token() {
cat <<-EOF
{
   "Description": "default agent token for '$(hostname)'",
   "Policies": [
      {
         "Name": "dns-discovery-read"
      }
   ],
   "Local": false
}
EOF
}


function consul_server_bootstrap() {
    consul_install
    consul_create_user
    consul_init_env

    consul_server_config > /etc/consul/consul-server.json
    consul_server_systemd_config > /etc/systemd/system/consul-server.service

    systemctl daemon-reload
    systemctl restart consul-server

    until [[ $(curl -s http://127.0.0.1:8500/v1/status/peers | jq length) -eq 3 ]]; do
      echo "waiting for consul cluster"
      sleep 5
    done

    export CONSUL_HTTP_TOKEN
    CONSUL_HTTP_TOKEN=$(config '.consul_master_token')
}

function consul_reload() {
    CONSUL_HTTP_TOKEN=$(config '.consul_master_token') consul reload
}

function consul_init_env() {
    mkdir -p /etc/consul
    mkdir -p "${STORAGE_LOCAL_DIR}/consul"
    chown consul "${STORAGE_LOCAL_DIR}/consul"
}

function consul_agent_bootstrap() {
    consul_install
    consul_create_user
    consul_init_env

    consul_agent_config > /etc/consul/consul-agent.json
    consul_agent_systemd_config > /etc/systemd/system/consul-agent.service

    systemctl daemon-reload
    systemctl restart consul-agent

    until [[ $(curl -s http://127.0.0.1:8500/v1/status/peers | jq length) -eq 3 ]]; do
      echo "waiting for consul cluster"
      sleep 5
    done
}

function consul_agent_set_resolver() {
cat <<-EOF >/etc/resolv.conf
  nameserver 127.0.0.1
EOF
}
#######################################
# hetzner-api.sh                      #
#######################################

HETZNER_CLOUD_API_URL=${HETZNER_CLOUD_API_URL:-https://api.hetzner.cloud}
HETZNER_CLOUD_API_TOKEN="${HETZNER_CLOUD_API_TOKEN:-$(config '.hetzner_cloud_api_token')}"

function hetzner_api_call() {
  curl --silent -H "Authorization: Bearer ${HETZNER_CLOUD_API_TOKEN}" "$@"
}

function hetzner_controller_public_ips {
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=role=controller" | jq -cr '.servers | map(.public_net.ipv4.ip)'
}

function hetzner_controller_hosts {
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=role=controller" | jq -cr '.servers | map([.public_net.ipv4.ip, .name] | join("  "))  | join("\n")'
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

#######################################
# solidblocks-management.sh           #
#######################################

function solidblocks_management_consul_config() {
    local role=${1:-}
cat <<-EOF
{
  "service": {
    "address": "",
    "checks": [
      {
        "interval": "10s",
        "http": "http://localhost:8080/api/v1/health",
        "timeout": "5s"
      }
    ],
    "name": "${role}-api",
    "port": 8080,
    "tags": ["${role}-api"]
  }
}
EOF
}


function solidblocks_management_systemd_config() {
    local command=${1:-}
cat <<-EOF
[Unit]
Description=solidblocks management
Requires=network-online.target
After=network-online.target

[Service]
EnvironmentFile=-/etc/sysconfig/solidblocks-management
Restart=always
RestartSec=5
ExecStart=/usr/local/bin/solidblocks-management.sh ${command}
ExecReload=/bin/kill -HUP $MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function solidblocks_management_bootstrap() {
    local role=${1:-}
    # openjdk-11-jre-headless currently has ca issues when accesing https endpoints
    check_and_install "openjdk-8-jre-headless"

    wget https://releases.solidblocks.de/solidblocks-management.sh -O /usr/local/bin/solidblocks-management.sh
    chmod +x /usr/local/bin/solidblocks-management.sh
    solidblocks_management_systemd_config "${role}" > /etc/systemd/system/solidblocks-management.service

    solidblocks_management_consul_config "${role}" > /etc/consul/service_solidblocks_management.json
    consul_reload

    systemctl daemon-reload
    
    if [[ ! ${DISABLE_MANAGEMENT} -eq 1 ]];  then
        systemctl enable solidblocks-management
        systemctl restart solidblocks-management
    fi

}

#######################################
# dnsmasq.sh                          #
#######################################


function dnsmasq_controller_config() {
    local domain
    domain="$(config '.domain')"
    local cloudname
    cloudname="$(config '.cloud_name')"
cat <<-EOF
no-poll
no-resolv
addn-hosts=/etc/${cloudname}.hosts
server=/${domain}/127.0.0.1#5353
server=/consul/127.0.0.1#8553
server=213.133.100.100
server=213.133.98.98
server=213.133.99.99
interface=eth0
bogus-priv
no-dhcp-interface=eth0
bind-dynamic
log-queries
cache-size=0
no-negcache
EOF
}

function dnsmasq_node_config() {
    local cloudname
    cloudname="$(config '.cloud_name')"
cat <<-EOF
no-poll
no-resolv
addn-hosts=/etc/${cloudname}.hosts
server=/consul/127.0.0.1#8553
server=213.133.100.100
server=213.133.98.98
server=213.133.99.99
interface=eth0
bogus-priv
no-dhcp-interface=eth0
bind-dynamic
log-queries
cache-size=0
no-negcache
EOF
}

function dnsmasq_controller_bootstrap() {
    check_and_install "dnsmasq"
    hetzner_controller_hosts > "/etc/$(config '.cloud_name').hosts"
    dnsmasq_controller_config > /etc/dnsmasq.conf
    systemctl restart dnsmasq
    dnsmasq_set_resolver
}

function dnsmasq_node_bootstrap() {
    check_and_install "dnsmasq"
    hetzner_controller_hosts > "/etc/$(config '.cloud_name').hosts"
    dnsmasq_node_config > /etc/dnsmasq.conf
    systemctl restart dnsmasq
    dnsmasq_set_resolver
}

function dnsmasq_set_resolver() {
cat <<-EOF >/etc/resolv.conf
  nameserver 127.0.0.1
EOF
cat <<-EOF >/etc/resolv.conf1
  nameserver 127.0.0.1
EOF
}
function fluentd_config() {
    local cloud_name
    cloud_name="$(config '.cloud_name')"
cat <<-EOF
<source>
  @type systemd
  tag systemd
  path /run/log/journal
  matches [
    { "_SYSTEMD_UNIT": "solidblocks-management.service" },
    { "_SYSTEMD_UNIT": "consul-server.service" },
    { "_SYSTEMD_UNIT": "consul-agent.service" }
  ]
  read_from_head false

  <storage>
    @type local
  </storage>

  <entry>
    fields_strip_underscores true
    fields_lowercase true
  </entry>
</source>

<filter *>
  @type record_transformer
  <record>
    hostname "#{Socket.gethostname}"
    cloud_name "${cloud_name}"
  </record>
</filter>

<match **>
  @type logzio_buffered
  endpoint_url https://listener.logz.io:8071?token=QxOtjGifJZftQUmkgoLNdZlYhLBvOoCZ&type=my_type
  output_include_time true
  output_include_tags true
  http_idle_timeout 10
  <buffer>
      @type memory
      flush_thread_count 4
      flush_interval 3s
      chunk_limit_size 16m      # Logz.io bulk limit is decoupled from chunk_limit_size. Set whatever you want.
      queue_limit_length 4096
  </buffer>
</match>

<system>
  root_dir /var/log/fluentd
</system>
EOF
}

function fluentd_systemd_config() {
cat <<-EOF
[Unit]
Description=Fluentd
Documentation=http://www.fluentd.org/
After=network.target

[Service]
Type=forking
ExecStart=/usr/local/bin/fluentd -d /var/run/fluentd.pid -c /etc/fluentd/fluentd.conf
PIDFile=/var/run/fluentd.pid
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
}

function fluentd_bootstrap() {
    check_and_install "ruby"
    check_and_install "ruby-dev"
    check_and_install "build-essential"

    gem install rake --no-ri --no-rdoc
    gem install fluent --no-ri --no-rdoc
    gem install fluent-plugin-logzio --no-ri --no-rdoc
    gem install fluent-plugin-systemd --no-ri --no-rdoc

    mkdir -p /etc/fluentd
    fluentd_config > /etc/fluentd/fluentd.conf
    fluentd_systemd_config > /etc/systemd/system/fluentd.service

    systemctl daemon-reload

    systemctl enable fluentd
    systemctl restart fluentd

}

function storage_init() {
    local volume_device="${1:-}"
    local volume_device_partition="${volume_device}1"
    local mount_name="${2:-}"
    local vg_name="vg_${mount_name}"
    local lv_name="lv_${mount_name}"
    local lvm_device="/dev/${vg_name}/${lv_name}"

    if parted -lm 2>&1 | grep -q "Error: ${volume_device}: unrecognised disk label" ; then
        echo "initializing disk '${volume_device}'"

        parted -s -- "${volume_device}" mktable gpt
        parted -s -- "${volume_device}" mkpart primary 2048s 100%
        parted -s -- "${volume_device}" set 1 lvm on

        while [[ ! -b "${volume_device_partition}" ]]
        do
            partprobe
            sleep 5
        done

        pvcreate "${volume_device_partition}"
        vgcreate "${vg_name}" "${volume_device_partition}"
        lvcreate --thin -l 99%VG -n "${lv_name}" "${vg_name}"
        mkfs.xfs -i size=512 "${lvm_device}"
    else
        echo "disk '${volume_device}' is already initialized"
    fi
}

function storage_init_lvm() {
    local mount_name="${1:-}"
    local mount_dir="${2}/${mount_name}"
    local vg_name="vg_${mount_name}"
    local lv_name="lv_${mount_name}"
    local lvm_device="/dev/${vg_name}/${lv_name}"

    if ! lvscan | grep -q "${vg_name}" | grep -q  "ACTIVE"; then
        echo "importing volume group '${vg_name}'"
        vgexport "${vg_name}" || true
        vgimport "${vg_name}"
        vgchange -ay "${vg_name}"
        lvchange -ay "/dev/${vg_name}/${lv_name}"
    fi

    # wait for lvm device to appear before mounting it
    while [[ ! -b "${lvm_device}" ]]
    do
        echo "waiting for lvm device '${lvm_device}'"
        sleep 5
    done

    # mount lvm device
    storage_create_dir "${mount_dir}"

    echo "${lvm_device} ${mount_dir} xfs inode64,nobarrier 0 0" >> /etc/fstab
    storage_mount_if_needed "${mount_dir}"
}


function storage_mount_if_needed() {
    local mount_dir="${1:-}"

    if ! grep -q "${mount_dir}" /proc/mounts ; then
        echo "mounting '${mount_dir}'"

        while ! mount "${mount_dir}" && ! grep -q "${mount_dir}" /proc/mounts; do
            echo "mounting '${mount_dir}' failed, retrying"
            sleep 5
        done
    else
        echo "'${mount_dir}' already mounted"
    fi
}

function storage_create_mount_if_needed() {
    local mount_dir="${1:-}"
    local fstab_entry="${2:-}"

    if ! grep -q "${mount_dir}" /etc/fstab; then
        echo "adding fstab entry for '${mount_dir}'"
        echo "${fstab_entry}" >> /etc/fstab
    fi
}

function storage_create_dir() {
    local dir="${1:-}"
    if [[ ! -d "${dir}" ]]; then
        echo "creating dir '${dir}'"
        mkdir -p "${dir}"
    fi
}


#######################################
# docker.sh                           #
#######################################

function install_docker() {
    curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
    apt-get update
    check_and_install "docker-ce"
}

function docker_login() {
    config '.registry_password' | docker login --username "$(config '.registry_username')" --password-stdin registry.gitlab.com
}

function bootstrap_docker_manager() {
    check_and_install "apt-transport-https"
    check_and_install "ca-certificates"
    check_and_install "curl"
    check_and_install "jq"
    check_and_install "gnupg2"
    check_and_install "software-properties-common"
    install_docker
    docker_login

    if [ "${DEBUG_LEVEL}" -gt 0 ]; then 
        sed -i 's/^ExecStart.*$/ExecStart=\/usr\/bin\/dockerd -H unix:\/\/ -H tcp:\/\/127.0.0.1:2375/' /lib/systemd/system/docker.service
        systemctl daemon-reload
        systemctl restart docker
    fi

    export CONSUL_HTTP_TOKEN
    CONSUL_HTTP_TOKEN="$(config '.consul_master_token')"
    
    if [[ "$(consul kv get "solidblocks/swarm/initialized")" == "${SOLIDBLOCKS_INSTANCE_ID}" ]]; then
        wait_for_consul_key "solidblocks/swarm/bootstrap/manager_join_command"
        local manager_join_command
        manager_join_command="$(consul kv get 'solidblocks/swarm/bootstrap/manager_join_command')"
        ${manager_join_command}
    else
        consul kv delete "solidblocks/swarm"
        consul kv put "solidblocks/swarm/initialized" "${SOLIDBLOCKS_INSTANCE_ID}"

        docker swarm init --advertise-addr "$(hetzner_get_own_public_ip)"
        docker network create -d overlay "$(config '.cloud_name')"

        local worker_join_command
        worker_join_command=$(docker swarm join-token worker | grep "docker swarm")
        local manager_join_command
        manager_join_command=$(docker swarm join-token manager | grep "docker swarm")

        consul kv put "solidblocks/swarm/bootstrap/worker_join_command" "${worker_join_command}"
        consul kv put "solidblocks/swarm/bootstrap/manager_join_command" "${manager_join_command}"
    fi

    docker node update --label-add role=controller "$(hostname)"
}
 
function bootstrap_docker_worker() {
    check_and_install "apt-transport-https"
    check_and_install "ca-certificates"
    check_and_install "curl"
    check_and_install "jq"
    check_and_install "gnupg2"
    check_and_install "software-properties-common"
    install_docker
    docker_login

    export CONSUL_HTTP_TOKEN
    CONSUL_HTTP_TOKEN="$(config '.consul_master_token')"
    
    wait_for_consul_key "solidblocks/swarm/bootstrap/worker_join_command"
    local worker_join_command
    worker_join_command="$(consul kv get 'solidblocks/swarm/bootstrap/worker_join_command')"
    ${worker_join_command}
}
 

#######################################
# cloud-init-controller.sh            #
#######################################

export STORAGE_LOCAL_NAME="local1"
export STORAGE_LOCAL_MOUNT_DIR="/storage/local"
export STORAGE_LOCAL_DIR="${STORAGE_LOCAL_MOUNT_DIR}/${STORAGE_LOCAL_NAME}"

function init_local_storage() {

    check_and_install "parted"
    check_and_install "xfsprogs"
    check_and_install "lvm2"
    check_and_install "thin-provisioning-tools"

    local real_device
    real_device="$(readlink -f "${STORAGE_LOCAL_DEVICE}")"
    storage_init "${real_device}" "${STORAGE_LOCAL_NAME}"
    storage_init_lvm "${STORAGE_LOCAL_NAME}" "${STORAGE_LOCAL_MOUNT_DIR}"
}

ca_install_root_cert
ca_ensure_certificates_dir
ca_create_certificate "$(hostname)" "${SOLIDBLOCKS_CERTIFICATES_DIR}"

init_local_storage

dnsmasq_controller_bootstrap
consul_server_bootstrap

fluentd_bootstrap
bootstrap_docker_manager
solidblocks_management_bootstrap "controller"

for ip in $(jq -r '.[]' <<< "$(hetzner_floating_ips_for_role 'controller')"); do
    echo "adding ip address ${ip} to eth0"
    ip addr add "${ip}" dev eth0
done


echo "done"
