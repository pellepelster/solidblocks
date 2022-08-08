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
  software_ensure_semver
  software_set_export_path
}

function task_build_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"
      sed -i "s/SOLIDBLOCKS_VERSION/${VERSION}/g" content/shell/installation/_index.md
      source ../solidblocks-shell/software.sh
      sed -i "s/TERRAFORM_VERSION/${TERRAFORM_VERSION}/g" content/shell/software/_index.md
      sed -i "s/CONSUL_VERSION/${CONSUL_VERSION}/g" content/shell/software/_index.md
      sed -i "s/HUGO_VERSION/${HUGO_VERSION}/g" content/shell/software/_index.md
      sed -i "s/SHELLCHECK_VERSION/${SHELLCHECK_VERSION}/g" content/shell/software/_index.md
      sed -i "s/SEMVER_VERSION/${SEMVER_VERSION}/g" content/shell/software/_index.md
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
  find "${DIR}/solidblocks-shell" -name "*.sh" -exec shellcheck {} \;
}

function task_release {
  ensure_environment

  if [[ ! -f ".semver.yaml" ]]; then
    semver init --release v0.0.1
  fi

  semver up release

  git tag -a "$(semver get release)" -m "$(semver get release)"
  git push --tags
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
  release) task_release "$@" ;;
  *) task_usage ;;
esac