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
COMPONENTS="solidblocks-test \
solidblocks-cli \
solidblocks-ansible \
solidblocks-ansible \
solidblocks-k3s-ansible \
solidblocks-shell \
solidblocks-cloud-init \
solidblocks-ssh \
solidblocks-rds-postgresql-docker \
solidblocks-rds-postgresql-ansible \
solidblocks-do-python \
solidblocks-web-s3-docker-hetzner \
solidblocks-rds-postgresql-hetzner"

mkdir -p "${TEMP_DIR}"

function clean_temp_dir {
  rm -rf "${TEMP_DIR}"
}
trap clean_temp_dir EXIT

function task_build {
    version_ensure "${VERSION}"

    for component in ${COMPONENTS}; do
      (
        echo "================================================================================="
        echo "running build for '${component}'"
        echo "================================================================================="
        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" build
      )
    done

    task_build_documentation
}

function task_format {
    for component in ${COMPONENTS}; do
      (
        echo "================================================================================="
        echo "running format for '${component}'"
        echo "================================================================================="
        cd "${DIR}/${component}"
        "./do" format
      )
    done
}

function task_clean_aws {
  aws-nuke run \
    --access-key-id "$(pass solidblocks/aws/admin/access_key_id)" \
    --secret-access-key "$(pass solidblocks/aws/admin/secret_access_key)" \
    --config ${DIR}/contrib/aws-nuke.yaml \
    --no-dry-run \
    --force
}

function task_test_init_aws {
  (
    export AWS_REGION="eu-central-1"
    export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/admin/access_key_id)}"
    export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/admin/secret_access_key)}"

    cd "${DIR}/contrib/terraform/test"
    terraform init -upgrade -migrate-state
    terraform apply -auto-approve
    terraform output
    terraform output test_access_key_secret
  )
}

function task_clean_hetzner {
  export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"
  ./solidblocks-cli-linux-amd64/blcks hetzner nuke --do-nuke
}

function task_clean_gcloud {
  trap clean_temp_dir EXIT
  pass solidblocks/gcp/test/service_account_key > "${TEMP_DIR}/service_account_key.json"
  gcloud auth activate-service-account --key-file "${TEMP_DIR}/service_account_key.json"

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
      echo "================================================================================="
      echo "running clean for '${component}'"
      echo "================================================================================="
        (
          cd "${DIR}/${component}"
          "./do" clean
        )
    done

    #task_clean_aws
    #task_clean_hetzner
    #task_clean_gcloud
}

function task_test {
    for component in ${COMPONENTS}; do
      (
        echo "================================================================================="
        echo "running test for '${component}'"
        echo "================================================================================="

        cd "${DIR}/${component}"
        VERSION=${VERSION} "./do" test
      )
    done
}

function prepare_documentation_env {
  local versions="$(grep  'VERSION=\".*\"' "${DIR}/solidblocks-shell/lib/software.sh")"
  for version in ${versions}; do
    eval "export ${version}"
  done
  export SOLIDBLOCKS_VERSION="$VERSION"
  export SOLIDBLOCKS_VERSION_RAW="${VERSION#"v"}"
}

function task_release {
  export VERSION="${1:-}"

  # ensure terraform-docs is available
  terraform-docs --version

  task_release_prepare ${VERSION}
  task_release_check ${VERSION}

  git tag -a "${VERSION}" -m "${VERSION}"
  git push --tags
}

function clean_temp_dir {
  rm -rf "${TEMP_DIR}"
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

function task_usage {
  echo "Usage: $0 ..."
  exit 1
}

ARG=${1:-}
shift || true

case ${ARG} in
  build) task_build "$@" ;;
  clean) task_clean "$@" ;;
  clean-aws) task_clean_aws "$@" ;;
  clean-hetzner) task_clean_hetzner "$@" ;;
  clean-gcloud) task_clean_gcloud "$@" ;;
  clean-cloud-resources) task_clean_hetzner && task_clean_aws "$@" ;;
  test-init-aws) task_test_init_aws "$@" ;;
  test) task_test "$@" ;;
  format) task_format "$@" ;;
  release) task_release "$@" ;;
  release-prepare) task_release_prepare "$@" ;;
  release-check) task_release_check "$@" ;;
  renovate) task_renovate "$@" ;;
  *) task_usage ;;
esac