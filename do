#!/usr/bin/env bash

set -eu

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/lib/download.sh"
source "${DIR}/solidblocks-shell/lib/software.sh"
source "${DIR}/solidblocks-shell/lib/file.sh"
source "${DIR}/solidblocks-shell/lib/log.sh"

VERSION="${GITHUB_REF_NAME:-snapshot}"
COMPONENTS="solidblocks-shell solidblocks-minio solidblocks-rds-postgresql"

function ensure_environment {
  software_ensure_shellcheck
  software_ensure_hugo
  software_ensure_semver
  software_set_export_path
}

function task_build {
    for component in ${COMPONENTS}; do
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" build
    done
}

function task_test {
    for component in ${COMPONENTS}; do
      (
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" test
      )
    done
}

function task_release_docker {
    for component in ${COMPONENTS}; do
      (
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" release-docker
      )
    done
}

function task_build_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"
      sed -i "s/TEMPLATE_SOLIDBLOCKS_SHELL_VERSION/${VERSION}/g" content/shell/installation/_index.md
      sed -i "s/TEMPLATE_SOLIDBLOCKS_SHELL_CHECKSUM/${SOLIDBLOCKS_SHELL_CHECKSUM:-}/g" content/shell/installation/_index.md
      source "${DIR}/solidblocks-shell/lib/software.sh"
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

function task_release {
  ensure_environment

  if [[ ! -f ".semver.yaml" ]]; then
    semver init --release v0.0.1
  fi

  local version="$(semver get release)"

  cat README_template.md | sed --expression "s/SOLIDBLOCKS_VERSION/${version}/g" > README.md
  #git add README.md
  #git commit -m "release ${version}"

  git tag -a "${version}" -m "${version}"
  git push --tags

  semver up release
  git add .semver.yaml
  git commit -m "bump version to $(semver get release)"
  git push
}


function task_usage {
  echo "Usage: $0 ..."
  exit 1
}

arg=${1:-}
shift || true
case ${arg} in
  build) task_build "$@" ;;
  test) task_test "$@" ;;
  build-documentation) task_build_documentation "$@" ;;
  serve-documentation) task_serve_documentation "$@" ;;
  release) task_release "$@" ;;
  release-docker) task_release_docker "$@" ;;
  *) task_usage ;;
esac