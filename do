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

  local version="$(semver get release)"

  cat README_template.md | sed --expression "s/SOLIDBLOCKS_VERSION/${version}/g" > README.md
  git add README.md
  git commit -m "release ${version}"

  git tag -a "${version}" -m "${version}"
  git push --tags

  semver up release
}

function task_test_shell {

  for test in ${DIR}/solidblocks-shell/test/test_*.sh; do
      log_divider_header ${test}
      ${test}
      log_divider_footer
  done

  find "${DIR}/solidblocks-shell/test/"  -name "test_*.sh" -exec {} \;
}

TESTCONTAINER_IMAGES="amazonlinux-2 debian-10 debian-11 ubuntu-20.04 ubuntu-22.04"

function task_test_shell_docker_prepare {
  (
    cd "${DIR}/solidblocks-shell/test/docker"

    for image in ${TESTCONTAINER_IMAGES}
    do
      docker build -t "solidblocks-testcontainer-${image}" -f "Dockerfile_${image}" .
    done
  )
}

function task_test_shell_docker {
  for image in ${TESTCONTAINER_IMAGES}
  do
      log_divider_header "running tests on '${image}'"
      docker run --network host -v /var/run/docker.sock:/var/run/docker.sock -v ${DIR}:/test "solidblocks-testcontainer-${image}" /test/do test-shell
      log_divider_footer
  done

}

function task_usage {
  echo "Usage: $0 ..."
  exit 1
}

arg=${1:-}
shift || true
case ${arg} in
  test-shell-docker-prepare) task_test_shell_docker_prepare "$@" ;;
  test-shell-docker) task_test_shell_docker "$@" ;;
  test-shell) task_test_shell "$@" ;;
  package-shell) task_package_shell "$@" ;;
  build-documentation) task_build_documentation "$@" ;;
  serve-documentation) task_serve_documentation "$@" ;;
  lint) task_lint "$@" ;;
  release) task_release "$@" ;;
  *) task_usage ;;
esac