#######################################
# dnsmasq.sh                          #
#######################################

function dnsmasq_external_systemd_config() {
cat <<-EOF
[Unit]
Description=dnsmasq-solidblocks-external
Requires=network-online.target
After=network-online.target

[Service]
EnvironmentFile=-/etc/sysconfig/dnsmasq-solidblocks-external
EnvironmentFile=-/solidblocks/instance/environment
Restart=always
RestartSec=5
ExecStart=/usr/sbin/dnsmasq -d -u dnsmasq --conf-file=/etc/dnsmasq-external.conf --local-service
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
}

function dnsmasq_controller_config() {
    local domain
    domain="$(config '.domain')"
cat <<-EOF
no-poll
no-resolv
addn-hosts=/etc/hosts.solidblocks
server=/${domain}/$(hetzner_get_own_public_ip)#5353
server=/consul/127.0.0.1#8553
server=213.133.100.100
server=213.133.98.98
server=213.133.99.99
listen-address=127.0.0.1,$(hetzner_get_own_private_ip)
interface=lo
bind-interfaces
no-dhcp-interface=eth0
log-queries
cache-size=0
no-negcache
EOF
}

function dnsmasq_external_controller_config() {
    local domain
    domain="$(config '.domain')"
cat <<-EOF
no-poll
no-resolv
server=/${domain}/$(hetzner_get_own_public_ip)#5353
interface=eth0
except-interface=lo
bogus-priv
no-dhcp-interface=eth0
bind-dynamic
log-queries
cache-size=0
no-negcache
EOF
}


function dnsmasq_node_config() {
cat <<-EOF
no-poll
no-resolv
addn-hosts=/etc/hosts.solidblocks
server=/consul/127.0.0.1#8553
server=213.133.100.100
server=213.133.98.98
server=213.133.99.99
interface=lo
bogus-priv
no-dhcp-interface=eth0
bind-dynamic
log-queries
cache-size=0
no-negcache
EOF
}

function dnsmasq_controller_hosts {
    hetzner_api_call "${HETZNER_CLOUD_API_URL}/v1/servers?label_selector=role=controller" | jq -cr --arg CLOUD_NAME "$(config '.cloud_name')" '.servers | map([.private_net[0].ip, .name, "mail", (.name + "." + $CLOUD_NAME)] | join("  "))  | join("\n")'
}


function dnsmasq_controller_bootstrap() {
    check_and_install "dnsmasq"
    check_and_install "dnsutils"

    dnsmasq_external_controller_config > /etc/dnsmasq-external.conf
    dnsmasq_external_systemd_config > /etc/systemd/system/dnsmasq-solidblocks-external.service

    dnsmasq_controller_config > /etc/dnsmasq.conf
    # workaround for non-working hosts rendering by consul template
    dnsmasq_controller_hosts > /etc/hosts.solidblocks
    cp /solidblocks/config/dnsmasq/dnsmasq-solidblocks.service /etc/systemd/system

    systemctl daemon-reload

    systemctl stop dnsmasq
    systemctl disable dnsmasq
    systemctl daemon-reload

    systemctl enable dnsmasq-solidblocks
    systemctl restart dnsmasq-solidblocks

    systemctl enable dnsmasq-solidblocks-external
    systemctl restart dnsmasq-solidblocks-external

    dnsmasq_set_resolver
}

function dnsmasq_wait() {
    local host=${1:-}
    while ! nslookup "${host}"; do
        echo "waiting for resolver to settle (${host})"
        sleep 5
    done
}

function dnsmasq_systemd_node_config() {
cat <<-EOF
[Unit]
Description=dnsmasq-solidblocks
Requires=network-online.target
After=network-online.target

[Service]
EnvironmentFile=-/etc/sysconfig/dnsmasq-solidblocks
EnvironmentFile=-/solidblocks/instance/environment
Restart=always
RestartSec=5
ExecStart=/usr/local/bin/consul-template -config /solidblocks/config/dnsmasq_consul-template-config.hcl
KillSignal=SIGINT

[Install]
WantedBy=multi-user.target
EOF
}

function dnsmasq_node_bootstrap() {
    check_and_install "dnsmasq"
    check_and_install "dnsutils"

    dnsmasq_node_config > /etc/dnsmasq.conf
    # workaround for non-working hosts rendering by consul template
    dnsmasq_controller_hosts > /etc/hosts.solidblocks
    cp /solidblocks/config/dnsmasq/dnsmasq-solidblocks.service /etc/systemd/system

    systemctl stop dnsmasq
    systemctl disable dnsmasq
    systemctl daemon-reload
    systemctl enable dnsmasq-solidblocks
    systemctl restart dnsmasq-solidblocks

    dnsmasq_set_resolver

    dnsmasq_wait "api.hetzner.cloud"
    dnsmasq_wait "controller-0.node.consul"
}

function dnsmasq_set_resolver() {
cat <<-EOF >/etc/resolv.conf
nameserver 127.0.0.1
search node.consul service.consul
EOF
chattr +i /etc/resolv.conf
}