vault {
  retry {
    enabled = true
    attempts = 0
    backoff = "250ms"
    max_backoff = "1m"
  }
}

log_level = "debug"

exec {
  command = "/solidblocks/bin/solidblocks-node-manager.sh"
}


template {
  source      = "/solidblocks/templates/ssh-config/known_hosts.ctmpl"
  destination = "/root/.ssh/known_hosts"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/protected/environment.ctmpl"
  destination = "/solidblocks/protected/environment"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/ssh-config/id_ed25519_signed.pub.ctmpl"
  destination = "/root/.ssh/id_ed25519_signed.pub"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/ssh-config/root_ssh_client_config.ctmpl"
  destination = "/root/.ssh/config"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/ssh-config/sshd_config.ctmpl"
  destination = "/etc/ssh/sshd_config"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/ssh-config/ssh_host_ed25519_key_signed.pub.ctmpl"
  destination = "/etc/ssh/ssh_host_ed25519_key_signed.pub"
  perms       = 0600
}

template {
  source      = "/solidblocks/templates/ssh-config/solidblocks_user_ssh_ca.pub.ctmpl"
  destination = "/etc/ssh/solidblocks_user_ssh_ca.pub"
  perms       = 0600
}


/*

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


*/