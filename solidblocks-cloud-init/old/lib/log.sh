function log() {
  local log_level=${1}
  shift || true

  local _message="${*}"

  local color_red
  local color_green
  local color_yellow
  local color_white
  local color_reset

  color_red="\x1B[31m"
  color_green="\x1B[32m"
  color_yellow="\x1B[33m"
  color_white="\x1B[37m"
  color_reset="\x1B[0m"
  local color="${color_white}"

  case "${log_level}" in
    info)       color="${color_white}" ;;
    debug)      color="${color_white}" ;;
    warning)    color="${color_yellow}" ;;
    success)    color="${color_green}" ;;
    error)      color="${color_red}" ;;
    emergency)  color="${color_red}" ;;
  esac

  echo -e "$(date -u +"%Y-%m-%d %H:%M:%S UTC") ${color}$(printf "[%9s]" "${log_level}")${color_reset} ${_message}" 1>&2
}

function log_divider_header() {
    echo "================================================================================"
    echo "$@"
    echo "--------------------------------------------------------------------------------"
}

function log_divider_footer() {
    echo "================================================================================"
    echo
}

function log_info () { log info "${*}"; }
function log_success() { log success "${*}"; }
function log_warning() { log warning "${*}"; }
function log_debug() { log debug "${*}"; }
function log_error() { log error "${*}"; }
function log_die  () { log emergency "${*}"; exit 1;}
