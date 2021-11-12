vault {
  renew_token = true

  retry {
    enabled = true
    attempts = 0
    backoff = "250ms"
    max_backoff = "1m"
  }
}

log_level = "debug"

exec {
  command = "/solidblocks/bin/node_manager.sh"
}

template {
  source      = "/solidblocks/templates/certificates.json.ctmpl"
  destination = "/solidblocks/certificates/certificates.json"
  perms       = 0600
  command     = "/solidblocks/bin/split_certificates.sh"

  wait {
    min = "2s"
    max = "4s"
  }
}

/*
template {
  source      = "/solidblocks/templates/mailname.ctmpl"
  destination = "/etc/mailname"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/postfix/main.cf.ctmpl"
  destination = "/etc/postfix/main.cf"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/postfix/sender_canonical.ctmpl"
  destination = "/etc/postfix/sender_canonical"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/postfix/smtp_auth.ctmpl"
  destination = "/etc/postfix/smtp_auth"
  perms       = 0600
}
*/