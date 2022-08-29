#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/colors.sh"

# see https://pellepelster.github.io/solidblocks/shell/log/#log_echo_error
function log_echo_error() {
  echo -e "${COLOR_RED}${*}${COLOR_RESET}" 1>&2;
}

function log_echo_warning() {
  echo -e "${COLOR_YELLOW}${*}${COLOR_RESET}" 1>&2;
}

# see https://pellepelster.github.io/solidblocks/shell/log/#log_echo_die
function log_echo_die() { log_echo_error "${*}"; exit 4;}

# see https://pellepelster.github.io/solidblocks/shell/log/#log_divider_header
function log_divider_header() {
    echo ""
    echo "================================================================================"
    log_info "$@"
    echo "--------------------------------------------------------------------------------"
}

# see https://pellepelster.github.io/solidblocks/shell/log/#log_divider_header
function log_divider_footer() {
    echo "================================================================================"
    echo
}

function log() {
  local log_level=${1}
  shift || true

  local _message="${*}"

  local color="${COLOR_WHITE}"

  case "${log_level}" in
    info)       color="${COLOR_BLACK}" ;;
    debug)      color="${COLOR_CYAN}" ;;
    warning)    color="${COLOR_YELLOW}" ;;
    success)    color="${COLOR_GREEN}" ;;
    error)      color="${COLOR_RED}" ;;
    emergency)  color="${COLOR_RED}" ;;
  esac

  echo -e "$(date -u +"%Y-%m-%d %H:%M:%S UTC") ${color}$(printf "[%9s]" "${log_level}")${COLOR_RESET} ${_message}" 1>&2
}

# see https://pellepelster.github.io/solidblocks/shell/log/#log
function log_info () { log info "${*}"; }
function log_success() { log success "${*}"; }
function log_warning() { log warning "${*}"; }
function log_debug() { log debug "${*}"; }
function log_error() { log error "${*}"; }

# see https://pellepelster.github.io/solidblocks/shell/log/#log_die
function log_die() { log emergency "${*}"; exit 4;}
