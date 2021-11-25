#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

function task_cli() {
    "${DIR}/gradlew" solidblocks-cli:run --args="$*"
}

function task_increment_version() {
    echo "SNAPSHOT-$(date +%Y%m%d%H%M%S)" > "${DIR}/version.txt"
}

function task_recreate_integration_test() {
  local db_dir="${DIR}/integration-test"
  rm -rf "${db_dir}"

  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" cloud config create --cloud blcks --domain blcks.de
  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" \
    cloud config create-environment \
      --cloud blcks \
      --environment dev \
      --hetzner-cloud-api-token-read-only "$(pass solidblocks/integration-test/hcloud_api_token_ro)" \
      --hetzner-cloud-api-token-read-write "$(pass solidblocks/integration-test/hcloud_api_token_rw)" \
      --github-read-only-token "$(pass solidblocks/github/personal_access_token_ro)" \
      --hetzner-dns-api-token "$(pass solidblocks/integration-test/dns_api_token)"

  "${DIR}/do" cli --db-password "$(pass solidblocks/integration-test/db_password)" --db-path "${db_dir}" cloud config ssh-config --cloud blcks
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
  increment-version) task_increment_version "$@" ;;
  recreate-integration-test) task_recreate_integration_test "$@" ;;
  publish) task_publish "$@" ;;
esac
