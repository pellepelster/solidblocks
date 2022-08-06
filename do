#!/usr/bin/env bash

set -eu

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/download.sh"
source "${DIR}/solidblocks-shell/software.sh"
source "${DIR}/solidblocks-shell/file.sh"

VERSION="${GITHUB_REF_NAME:-snapshot}"


function ensure_environment {
  software_ensure_shellcheck
  software_ensure_hugo
  software_ensure_export_path
}

function task_build_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"
      sed -i "s/SOLIDBLOCKS_VERSION/${VERSION}/g" content/shell/installation/_index.md
      hugo
    )
}

function task_build_shell {
  (
    cd ${DIR}
    zip -r "solidblocks-shell-${VERSION}.zip" solidblocks-shell/*.sh
  )
}

function task_serve_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"
      hugo serve --baseURL "/"
    )
}

function task_lint {
  ensure_environment
  find "${DIR}/solidblocks-shell" -exec shellcheck {} \;
}

function task_test {
  "${DIR}/solidblocks-shell/test/test_download.sh"
}

function task_usage {
  echo "Usage: $0 ..."
  exit 1
}

arg=${1:-}
shift || true
case ${arg} in
  build-shell) task_build_shell "$@" ;;
  build-documentation) task_build_documentation "$@" ;;
  serve-documentation) task_serve_documentation "$@" ;;
  lint) task_lint "$@" ;;
  test) task_test "$@" ;;
  *) task_usage ;;
esac