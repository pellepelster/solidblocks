#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/lib/download.sh"
source "${DIR}/solidblocks-shell/lib/software.sh"
source "${DIR}/solidblocks-shell/lib/file.sh"
source "${DIR}/solidblocks-shell/lib/log.sh"
source "${DIR}/lib/terraform.sh"
source "${DIR}/lib/utils.sh"

export VERSION="$(version)"

TEMP_DIR="${DIR}/.temp"
COMPONENTS="solidblocks-cli solidblocks-test solidblocks-ansible solidblocks-k3s solidblocks-hetzner-dns solidblocks-terraform solidblocks-shell solidblocks-cloud-init solidblocks-hetzner solidblocks-debug-container solidblocks-sshd solidblocks-rds-postgresql-docker solidblocks-rds-postgresql-ansible"

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

function task_release_prepare {
  export VERSION="${1:-}"

  if [[ -z "${VERSION}" ]]; then
    echo "no version set"
    exit 1
  fi

  for component in ${COMPONENTS}; do
    (
      echo "running release-prepare for '${component}'"
      cd "${DIR}/${component}"
      "./do" release-prepare "${VERSION}"
    )
  done
}

function task_release_test {
  for component in ${COMPONENTS}; do
    (
      echo "running release-test for '${component}'"
      cd "${DIR}/${component}"
      "./do" release-test
    )
  done
}

function task_clean_aws {

  #export AWS_REGION="eu-central-1"
  #export AWS_ACCESS_KEY_ID="$(pass solidblocks/aws/admin/access_key)"
  #export AWS_SECRET_ACCESS_KEY="$(pass solidblocks/aws/admin/secret_access_key)"
  #aws s3 ls | cut -d" " -f 3 | xargs -I{} aws s3 rb s3://{} --force

  docker run \
    --rm \
    -v $(pwd)/contrib/aws-nuke.yaml:/home/aws-nuke/config.yml \
    quay.io/rebuy/aws-nuke:v2.25.0 \
    --access-key-id "$(pass solidblocks/aws/admin/access_key)" \
    --secret-access-key "$(pass solidblocks/aws/admin/secret_access_key)" \
    --config /home/aws-nuke/config.yml \
    --no-dry-run \
    --force

}

function task_clean_hetzner {
  export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"
  ./solidblocks-cli/blcks-linuxX64-${VERSION} hetzner nuke --do-nuke
}

function task_clean_gcloud {
  for bucket in $(gcloud storage ls); do
    if [[ ${bucket} = gs://test-* ]]; then
      echo "deleting bucket '${bucket}'"
      gcloud storage rm --recursive "${bucket}"
    else
      echo "not deleting bucket '${bucket}'"
    fi
  done
}

function task_clean {
    rm -rf "${DIR}/build"
    rm -rf "${DIR}/doc/generated"
    rm -rf "${DIR}/doc/snippets"

    for component in ${COMPONENTS}; do
        (
          cd "${DIR}/${component}"
          "./do" clean
        )
    done

    task_clean_aws
    task_clean_hetzner
    task_clean_gcloud
}

function task_test_init {
  #(
  #  cd "${DIR}/testbeds/gcs"
  #  terraform init -upgrade
  #  terraform apply -auto-approve
  #)
  echo ""
  #terraform_wrapper "${DIR}/testbeds/hetzner/bootstrap" apply -auto-approve
}

function task_test {
    if [[ "${SKIP_TESTS:-}" == "true" ]]; then
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

function task_release_artifacts {
    for component in ${COMPONENTS}; do
      (
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" release-artifacts
      )
    done
}

function prepare_documentation_env {
  local versions="$(grep  'VERSION=\".*\"' "${DIR}/solidblocks-shell/lib/software.sh")"
  for version in ${versions}; do
    eval "export ${version}"
  done
  export SOLIDBLOCKS_VERSION="${VERSION}"
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

    mkdir -p "${DIR}/build/documentation"
    (
      cd "${DIR}/build/documentation"
      cp -r ${DIR}/doc/* ./
      prepare_documentation_env
      hugo
    )
}

function task_serve_documentation {
    ensure_environment
    (
      cd "${DIR}/doc"

      prepare_documentation_env
      hugo serve --baseURL "/"
    )
}

function task_bootstrap() {
  git submodule update --init --recursive
  software_ensure_shellcheck
  software_ensure_hugo
}

function task_release_check() {
  local previous_tag="$(git --no-pager tag | sed '/-/!{s/$/_/}' | sort -V | sed 's/_$//' | tail -1)"
  local previous_version="${previous_tag#v}"

  export VERSION="${1:-}"

  if [[ -z "${VERSION}" ]]; then
    echo "no version set"
    exit 1
  fi

  echo "checking for release version '${VERSION}'"

  task_build
  task_build_documentation

  local previous_version_escaped="${previous_version//\./\\.}"
  echo "checking for previous version '${previous_version}'"

  if git --no-pager grep "${previous_version_escaped}" | grep -v CHANGELOG.md | grep -v README.md; then
    echo "previous version '${previous_version_escaped}' found in repository"
    exit 1
  fi

  if [[ "${VERSION}" == *"pre"* ]]; then
    echo "skipping changelog check for pre-version '${VERSION}'"
  else
    echo "checking changelog for current version '${VERSION}'"
    if ! grep "${VERSION}" "${DIR}/CHANGELOG.md"; then
      echo "version '${VERSION}' not found in changelog"
      exit 1
    fi
  fi

  if [[ $(git diff --stat) != '' ]]; then
    echo "repository '${DIR}' is dirty"
    exit 1
  fi
}

function task_release {
  export VERSION="${1:-}"

  # ensure terraform-docs is available
  terraform-docs --version

  task_release_check ${VERSION}

  git tag -a "v${VERSION}" -m "v${VERSION}"
  git push --tags
}

function clean_temp_dir {
  rm -rf "${TEMP_DIR}"
}

function task_release_tf_modules {
  local version="${1:-}"
  task_release_tf_module "terraform-null-solidblocks-cloud-init" "${version}"
  task_release_tf_module "terraform-hcloud-solidblocks-rds-postgresql" "${version}"
}

function task_renovate {
  docker run --rm \
    -e RENOVATE_PLATFORM=github \
    -e RENOVATE_TOKEN="$(pass github/pelle/pat)" \
    -e RENOVATE_AUTODISCOVER=false \
    -e RENOVATE_BASE_DIR=/tmp/renovate \
    -e RENOVATE_CONFIG_FILE=/renovate.json \
    -v $(pwd)/renovate.json:/renovate.json \
    renovate/renovate:35.14.4
}

function task_release_tf_module {
  local module="${1:-}"
  local version="${2:-}"

  clean_temp_dir

  mkdir -p "${TEMP_DIR}/${module}/git"
  git clone "git@github.com:pellepelster/${module}.git" "${TEMP_DIR}/${module}/git"

  mkdir -p "${TEMP_DIR}/${module}/sources"
  curl -L "https://github.com/pellepelster/solidblocks/releases/download/v${version}/${module}-v${version}.zip" -o "${TEMP_DIR}/${module}/${module}.zip"
  unzip "${TEMP_DIR}/${module}/${module}.zip" -d "${TEMP_DIR}/${module}/sources"
  cp -rv ${TEMP_DIR}/${module}/sources/* "${TEMP_DIR}/${module}/git"
  (
    cd "${TEMP_DIR}/${module}/git"
    git add -A
    git commit -m "version ${version}" || true
    git push

    git tag -a "${version}" -m "${version}"
    git push --tags
  )
  # git tag -d "v${VERSION}" && git push origin ":refs/tags/v${VERSION}" && git tag -a "v${VERSION}" -m "v${VERSION}" && git push --tags
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
  clean-gcloud) task_clean_gcloud "$@" ;;
  clean-cloud-resources) task_clean_hetzner && task_clean_aws "$@" ;;
  test-init) task_test_init "$@" ;;
  test) task_test "$@" ;;
  format) task_format "$@" ;;
  build-documentation) task_build_documentation "$@" ;;
  serve-documentation) task_serve_documentation "$@" ;;
  release) task_release "$@" ;;
  release-artifacts) task_release_artifacts "$@" ;;
  release-prepare) task_release_prepare "$@" ;;
  release-check) task_release_check "$@" ;;
  release-tf-modules) task_release_tf_modules "$@" ;;
  release-test) task_release_test "$@" ;;
  bootstrap) task_bootstrap "$@" ;;
  renovate) task_renovate "$@" ;;
  *) task_usage ;;
esac