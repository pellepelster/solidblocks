function network_ufw_before_rules() {
local own_floating_ip
own_floating_ip="$(hetzner_get_own_floating_ip)"
local own_ip
own_ip="$(hetzner_get_own_public_ip)"
cat <<-EOF
*nat
:PREROUTING ACCEPT [0:0]
-A PREROUTING -d ${own_floating_ip} -p tcp --dport 80 -j  DNAT --to-destination ${own_ip}:80
-A PREROUTING -d ${own_floating_ip} -p tcp --dport 443 -j  DNAT --to-destination ${own_ip}:443
COMMIT
EOF
}

function network_firewall_controller_setup() {
    check_and_install "ufw"
    ufw allow 22/tcp
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw allow 53/udp

    ufw default allow incoming

    network_ufw_before_rules >> /etc/ufw/before.rules
    ufw enable
    systemctl restart ufw
}