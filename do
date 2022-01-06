#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

function task_cli() {
    "${DIR}/gradlew" solidblocks-cli:run --args="$*"
}

function task_increment_version() {
    echo "SNAPSHOT-$(date +%Y%m%d%H%M%S)" > "${DIR}/version.txt"
}

function task_prepare_service_base_integration_test() {
    local timestamp_now=$(date +%Y%m%d%H%M%S)

    local solidblocks_blue_version="BLUE-${timestamp_now}"
    local solidblocks_green_version="GREEN-${timestamp_now}"

    local distributions_dir="${DIR}/solidblocks-service-base-integrationtest/build/distributions"
    local artefacts_dir="${DIR}/solidblocks-service-base-integrationtest/src/test/resources/service-integrationtest/bootstrap/artefacts"

    mkdir -p "${artefacts_dir}"
    rm -rf ${artefacts_dir}/*.tar
    rm -rf ${artefacts_dir}/*.version

    SOLIDBLOCKS_VERSION="${solidblocks_blue_version}" "${DIR}/gradlew" solidblocks-service-base-integrationtest:assemble
    SOLIDBLOCKS_VERSION="${solidblocks_green_version}" "${DIR}/gradlew" solidblocks-service-base-integrationtest:assemble

    cp "${distributions_dir}/solidblocks-service-base-integrationtest-${solidblocks_blue_version}.tar" "${artefacts_dir}/solidblocks-service-base-integrationtest-${solidblocks_blue_version}.tar"
    cp "${distributions_dir}/solidblocks-service-base-integrationtest-${solidblocks_green_version}.tar" "${artefacts_dir}/solidblocks-service-base-integrationtest-${solidblocks_green_version}.tar"

    echo "${solidblocks_blue_version}" > "${artefacts_dir}/blue.version"
    echo "${solidblocks_green_version}" > "${artefacts_dir}/green.version"

    #docker-compose -f solidblocks-service-base-integrationtest/docker-compose.yml build \
    #  --build-arg SOLIDBLOCKS_BLUE_VERSION="${solidblocks_blue_version}" \
    #  --build-arg SOLIDBLOCKS_GREEN_VERSION="${solidblocks_green_version}"

    #rm -rf "${test_base_dir}"
    #mkdir -p "${test_base_dir}/instance"
    #echo "SOLIDBLOCKS_VERSION=${solidblocks_blue_version}" > "${test_base_dir}/instance/environment"

    #docker-compose -f solidblocks-service-base-integrationtest/docker-compose.yml up

}

function task_recreate_integration_test() {
  local db_dir="${DIR}/integration-test"
  rm -rf "${db_dir}"

  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" \
    cloud create \
    --cloud blcks \
    --domain blcks.de

  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" \
    environment create \
      --cloud blcks \
      --environment dev \
      --hetzner-cloud-api-token-read-only "$(pass solidblocks/integration-test/hcloud_api_token_ro)" \
      --hetzner-cloud-api-token-read-write "$(pass solidblocks/integration-test/hcloud_api_token_rw)" \
      --github-read-only-token "$(pass solidblocks/github/personal_access_token_ro)" \
      --hetzner-dns-api-token "$(pass solidblocks/integration-test/dns_api_token)"

  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" \
    environment ssh-config \
    --cloud blcks \
    --environment dev

  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" \
    tenant create \
    --cloud blcks \
    --environment dev \
    --tenant tenant1
}

function task_publish() {
  export GITHUB_USERNAME="pellepelster"
  export GITHUB_TOKEN="$(pass solidblocks/github/personal_access_token_rw)"

  "${DIR}/gradlew" solidblocks-cloud-init:publish
}


COMMAND="${1:-}"
shift || true

case ${COMMAND} in
  cli) task_cli "$@" ;;
  prepare-service-base-integration-test) task_prepare_service_base_integration_test "$@" ;;
  increment-version) task_increment_version "$@" ;;
  recreate-integration-test) task_recreate_integration_test "$@" ;;
  publish) task_publish "$@" ;;
esac
