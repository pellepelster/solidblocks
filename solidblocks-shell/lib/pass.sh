#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/log.sh"

function pass_ensure_initialized() {

  if [[ -z ${SECRETS_DIR:-} ]]; then
      log_echo_die "environment variable 'SECRETS_DIR' missing"
  fi

  if ! PASSWORD_STORE_DIR="${SECRETS_DIR}" pass &> /dev/null; then
      log_echo_die "secret dir '${SECRETS_DIR}' not initialized"
  fi
}

function pass_wrapper() {

  if [[ -z ${SECRETS_DIR:-} ]]; then
      log_echo_die "environment variable 'SECRETS_DIR' missing"
      exit 1
  fi

  PASSWORD_STORE_DIR="${SECRETS_DIR}" pass $@
}

function pass_secret() {
  # shellcheck disable=SC2155
  local secret_env="$(echo "${1:-}" | tr "\/" "_" | tr "[:lower:]" "[:upper:]")"


  if [[ -z "${!secret_env:-}" ]]; then
    pass_wrapper "${1:-}"
  else
    log_echo_warning "reading secret from environment variable '${secret_env}'"
    echo "${!secret_env:-}"
  fi
}
