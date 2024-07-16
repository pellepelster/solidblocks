function ensure_command() {
    local command=${1:-}

    if ! type "${command}" &>/dev/null; then
      log_echo_die "command '${command}' not installed"
    fi
}
