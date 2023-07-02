#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/lib/download.sh"
source "${DIR}/solidblocks-shell/lib/software.sh"
source "${DIR}/solidblocks-shell/lib/file.sh"
source "${DIR}/solidblocks-shell/lib/log.sh"

VERSION="${GITHUB_REF_NAME:-snapshot}"

COMPONENTS="solidblocks-terraform solidblocks-hetzner-nuke solidblocks-shell solidblocks-cloud-init solidblocks-hetzner solidblocks-debug-container solidblocks-sshd solidblocks-minio solidblocks-rds-postgresql"

function ensure_environment {
  software_set_export_path
}

function task_build {
    for component in ${COMPONENTS}; do
      (
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" build
      )
    done

    task_build_documentation
}

function task_clean_aws {
  docker run \
    --rm -it \
    -v $(pwd)/contrib/aws-nuke.yaml:/home/aws-nuke/config.yml \
    quay.io/rebuy/aws-nuke:v2.22.1 \
    --access-key-id "$(pass solidblocks/aws/admin/access_key)" \
    --secret-access-key "$(pass solidblocks/aws/admin/secret_access_key)" \
    --config /home/aws-nuke/config.yml \
    --no-dry-run \
    --force

}

function task_clean_hetzner {
  docker run \
    --rm \
    -e HCLOUD_TOKEN="$(pass solidblocks/hetzner/hcloud_api_token)" \
    --pull always \
    ghcr.io/pellepelster/solidblocks-hetzner-nuke:v0.1.15 nuke
}

function task_clean {
    task_clean_aws
    task_clean_hetzner

    rm -rf "${DIR}/build"
    rm -rf "${DIR}/doc/generated"
    rm -rf "${DIR}/doc/snippets"

    for component in ${COMPONENTS}; do
        (
          cd "${DIR}/${component}"
          "./do" clean
        )
    done
}

function task_test {
    if [[ -n "${SKIP_TESTS:-}" ]]; then
      exit 0
    fi

    for component in ${COMPONENTS}; do
      (
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" test
      )
    done
}

function task_format {
    for component in ${COMPONENTS}; do
      (
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" format
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

    rm -rf "${DIR}/doc/snippets"
    mkdir -p "${DIR}/doc/snippets"

    if [[ -n "${CI:-}" ]]; then
      rsync -rv --exclude=".terraform" --exclude="*.tfstate*" --exclude=".terraform.lock.hcl" ${DIR}/*/snippets/* "${DIR}/doc/snippets"
    else
      rsync -rv --exclude=".terraform" --exclude="*.tfstate*" --exclude=".terraform.lock.hcl" ${DIR}/*/snippets/* "${DIR}/doc/snippets"
      rsync -rv --exclude=".terraform" --exclude="*.tfstate*" --exclude=".terraform.lock.hcl" ${DIR}/*/build/snippets/* "${DIR}/doc/snippets"
    fi

    export VERSION="$(semver get release)"
    mkdir -p "${DIR}/build/documentation"
    (
      cd "${DIR}/build/documentation"

      cp -r ${DIR}/doc/* ./

      source "${DIR}/solidblocks-shell/lib/software.sh"
      sed -i "s/__TERRAFORM_VERSION__/${TERRAFORM_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__CONSUL_VERSION__/${CONSUL_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__HUGO_VERSION__/${HUGO_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__SHELLCHECK_VERSION__/${SHELLCHECK_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__SEMVER_VERSION__/${SEMVER_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__TERRAGRUNT_VERSION__/${TERRAGRUNT_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__RESTIC_VERSION__/${RESTIC_VERSION}/g" content/shell/software/_index.md
      sed -i "s/__SOLIDBLOCKS_VERSION__/${VERSION}/g" content/rds/_index.md
      sed -i "s/__SOLIDBLOCKS_VERSION__/${VERSION}/g" content/hetzner/nuke.md
      sed -i "s/__SOLIDBLOCKS_VERSION__/${VERSION}/g" content/terraform/_index.md
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

function task_bootstrap() {
  software_ensure_shellcheck
  software_ensure_hugo
  software_ensure_semver
}

function task_release {

  # ensure terraform-docs is available
  terraform-docs --version

  local previous_tag="$(git describe --abbrev=0 --tags)"
  local previous_version="${previous_tag#v}"

  if [[ ! -f ".semver.yaml" ]]; then
    semver init --release v0.0.1
  fi

  task_build
  task_build_documentation

  if [[ $(git diff --stat) != '' ]]; then
    echo "repository '${DIR}' is dirty"
    exit 1
  fi

  local version="$(semver get release)"

  if ! grep "${version}" "${DIR}/CHANGELOG.md"; then
    echo "version '${version}' not found in changelog"
    exit 1
  fi

  if git --no-pager grep "${previous_version}" | grep -v CHANGELOG.md | grep -v README.md; then
    echo "previous version '${previous_version}' found in repository"
    exit 1
  fi

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

ARG=${1:-}
shift || true

case "${ARG}" in
  bootstrap) ;;
  *) ensure_environment ;;
esac

case ${ARG} in
  build) task_build "$@" ;;
  clean) task_clean "$@" ;;
  clean-aws) task_clean_aws "$@" ;;
  clean-hetzner) task_clean_hetzner "$@" ;;
  test) task_test "$@" ;;
  format) task_format "$@" ;;
  build-documentation) task_build_documentation "$@" ;;
  serve-documentation) task_serve_documentation "$@" ;;
  release) task_release "$@" ;;
  release-docker) task_release_docker "$@" ;;
  bootstrap) task_bootstrap "$@" ;;
  *) task_usage ;;
esac