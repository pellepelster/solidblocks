consul {
  ssl {
    enabled = false
  }

  retry {
    enabled = true
    attempts = 0
    backoff = "250ms"
    max_backoff = "1m"
  }
}

log_level = "debug"

exec {
  command = "/usr/sbin/dnsmasq -d -u dnsmasq --conf-file=/etc/dnsmasq.conf --local-service"
}

#template {
#  source      = "/solidblocks/templates/dnsmasq/hosts.ctmpl"
#  destination = "/etc/hosts.solidblocks"
#  perms       = 0755
#}
