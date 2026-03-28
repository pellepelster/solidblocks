_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

test -f "${_DIR}/curl.sh" && source "${_DIR}/curl.sh"

function restic_install() {
  curl_wrapper https://github.com/restic/restic/releases/download/v0.18.1/restic_0.18.1_linux_amd64.bz2 -o /tmp/restic_0.18.1_linux_amd64.bz2
  echo "680838f19d67151adba227e1570cdd8af12c19cf1735783ed1ba928bc41f363d /tmp/restic_0.18.1_linux_amd64.bz2" | sha256sum --check
  bunzip2 /tmp/restic_0.18.1_linux_amd64.bz2
  mv /tmp/restic_0.18.1_linux_amd64 /usr/bin/restic
  chmod +x /usr/bin/restic
}

function restic_ensure_local_repo() {
  local repo="${1}"
  export $(cat /etc/restic/credentials | xargs)

  if restic --repo "${repo}" cat config; then
    echo "[blcks] repository '${repo}' already exists"
  else
    echo "[blcks] initializing repository '${repo}'"
    restic init --repo "${repo}"
  fi
}

function restic_ensure_s3_repo() {
  local repo="${1}"
  export $(cat /etc/restic/credentials | xargs)

  echo "[blcks] checking repository '${repo}'"

  if restic --repo "${repo}" cat config; then
    echo "[blcks] repository '${repo}' already exists"
  else
    echo "[blcks] initializing repository '${repo}'"
    restic init --repo "${repo}"
  fi
}

function restic_backup() {
  local repo="${1}"
  local directory="${2}"
  export $(cat /etc/restic/credentials | xargs)

  restic --repo "${repo}" --verbose backup ${directory}
}

function restic_restore() {
  local repo="${1}"
  export $(cat /etc/restic/credentials | xargs)

  local snapshots=$(restic --repo ${repo} snapshots --json --latest 1)

    if [[ "${snapshots}" == "[]" ]]; then
      echo "[blcks] no snapshots found in repository '${repo}'"
      return
    fi

    local snapshot_id=$(echo ${snapshots} | jq -r ".[0].id")
    local snapshot_time=$(echo ${snapshots} | jq -r ".[0].time")
    echo "[blcks] found snapshot '${snapshot_id}' from '${snapshot_time}'"

    while read path
    do
      if [ -z "$( ls -A ${path} )" ]; then
         echo "[blcks] target path '${path}' is empty"
      else
         echo "[blcks] target path '${path}' is not empty, canceling restore"
         return
      fi
    done < <(echo "${snapshots}" | jq -r '.[0].paths[]')

    echo "[blcks] restoring snapshot '${snapshot_id}' from repository '${repo}'"
    restic --repo "${repo}" restore --overwrite never --target / --json ${snapshot_id}
}

function restic_stats() {
  local repo="${1}"
  export $(cat /etc/restic/credentials | xargs)

  restic --repo "${repo}" stats --json
}

function restic_snapshots() {
  local repo="${1}"
  shift || true
  export $(cat /etc/restic/credentials | xargs)

  restic --repo "${repo}" snapshots --json $@
}
