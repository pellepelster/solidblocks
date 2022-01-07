#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

function task_cli() {
    "${DIR}/gradlew" solidblocks-cli:run --args="$*"
}

function task_increment_version() {
    echo "SNAPSHOT-$(date +%Y%m%d%H%M%S)" > "${DIR}/version.txt"
}

function task_helloworld_agent_integration_test() {
    local timestamp_now=$(date +%Y%m%d%H%M%S)

    local solidblocks_blue_version="BLUE-${timestamp_now}"
    local solidblocks_green_version="GREEN-${timestamp_now}"

    local distributions_dir="${DIR}/solidblocks-helloworld-agent/build/distributions"
    local artefacts_dir="${DIR}/solidblocks-helloworld-agent/src/test/resources/helloworld/bootstrap/artefacts"

    mkdir -p "${artefacts_dir}"
    rm -rf ${artefacts_dir}/*.tar
    rm -rf ${artefacts_dir}/*.version

    SOLIDBLOCKS_VERSION="${solidblocks_blue_version}" "${DIR}/gradlew" solidblocks-helloworld-agent:assemble
    SOLIDBLOCKS_VERSION="${solidblocks_green_version}" "${DIR}/gradlew" solidblocks-helloworld-agent:assemble

    cp "${distributions_dir}/solidblocks-helloworld-agent-${solidblocks_blue_version}.tar" "${artefacts_dir}/solidblocks-helloworld-agent-${solidblocks_blue_version}.tar"
    cp "${distributions_dir}/solidblocks-helloworld-agent-${solidblocks_green_version}.tar" "${artefacts_dir}/solidblocks-helloworld-agent-${solidblocks_green_version}.tar"

    echo "${solidblocks_blue_version}" > "${artefacts_dir}/blue.version"
    echo "${solidblocks_green_version}" > "${artefacts_dir}/green.version"
}

function task_test() {
  task_helloworld_agent_integration_test
  "${DIR}/gradlew" clean ktFormat check
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

  #"${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" \
  #  tenant create \
  #  --cloud blcks \
  #  --environment dev \
  #  --tenant tenant1

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
  test) task_test "$@" ;;
  prepare-helloworld-agent-integration-test) task_helloworld_agent_integration_test "$@" ;;
  increment-version) task_increment_version "$@" ;;
  recreate-integration-test) task_recreate_integration_test "$@" ;;
  publish) task_publish "$@" ;;
esac
