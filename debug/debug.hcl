vault {
  retry {
    enabled = true
    attempts = 0
    backoff = "250ms"
    max_backoff = "1m"
  }
  renew_token = false
}

log_level = "debug"

exec {
  command = "./debug.sh"
}


template {
  source      = "debug.ctmpl"
  destination = "debug.rendered"
  perms       = 0600
}
