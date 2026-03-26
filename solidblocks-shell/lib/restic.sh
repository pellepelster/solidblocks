_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

#test -f "${_DIR}/text.sh" && source "${_DIR}/text.sh"
#test -f "${_DIR}/utils.sh" && source "${_DIR}/utils.sh"

function restic_install() {
  apt-get install -y restic
}

function restic_ensure_repo() {
  local repo="${1}"
  local password="${2}"

  export RESTIC_PASSWORD="${password}"

  if restic --repo "${repo}" cat config; then
    echo "[blcks] repository '${repo}' already exists"
  else
    echo "[blcks] initializing repository '${repo}'"
    restic init --repo "${repo}"
  fi
}

function restic_backup() {
  local repo="${1}"
  local password="${2}"
  local directory="${3}"

  export RESTIC_PASSWORD="${password}"
  restic --repo "${repo}" --verbose backup ${directory}
}

function restic_restore() {
  local repo="${1}"
  local password="${2}"

  export RESTIC_PASSWORD="${password}"
  if [[ "$(restic --repo ${repo} snapshots --json)" == "[]" ]]; then
    echo "[blcks] no snapshots found in repository '${repo}'"
  else
    echo "[blcks] restoring latest snapshot from repository '${repo}'"
    restic --repo "${repo}" restore latest --target /
  fi
}

function restic_stats() {
  local repo="${1}"
  local password="${2}"

  export RESTIC_PASSWORD="${password}"
  restic --repo "${repo}" stats --json
}

function restic_snapshots() {
  local repo="${1}"
  local password="${2}"

  export RESTIC_PASSWORD="${password}"
  restic --repo "${repo}" snapshots --json
}
