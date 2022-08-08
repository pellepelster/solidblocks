#!/usr/bin/env bash

set -eu

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/download.sh"
source "${DIR}/solidblocks-shell/software.sh"
source "${DIR}/solidblocks-shell/file.sh"
source "${DIR}/solidblocks-shell/log.sh"

VERSION="${GITHUB_REF_NAME:-snapshot}"


function ensure_environment {
  software_ensure_shellcheck
  software_ensure_hugo
  software_set_export_path
}

function task_build_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"
      sed -i "s/SOLIDBLOCKS_VERSION/${VERSION}/g" content/shell/installation/_index.md
      hugo
    )
}

function task_serve_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"
      hugo serve --baseURL "/"
    )
}

function task_package_shell {
  (
    cd ${DIR}
    zip -r "solidblocks-shell-${VERSION}.zip" solidblocks-shell/*.sh
  )
}

function task_lint {
  ensure_environment
  find "${DIR}/solidblocks-shell" -exec shellcheck {} \;
}

function task_test_shell {

  for test in ${DIR}/solidblocks-shell/test/test_*.sh; do
      log_divider_header ${test}
      ${test}
      log_divider_footer
  done

  find "${DIR}/solidblocks-shell/test/"  -name "test_*.sh" -exec {} \;
}

function task_usage {
  echo "Usage: $0 ..."
  exit 1
}

arg=${1:-}
shift || true
case ${arg} in
  test-shell) task_test_shell "$@" ;;
  package-shell) task_package_shell "$@" ;;
  build-documentation) task_build_documentation "$@" ;;
  serve-documentation) task_serve_documentation "$@" ;;
  lint) task_lint "$@" ;;
  *) task_usage ;;
esac