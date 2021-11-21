#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

function task_cli() {
    "${DIR}/gradlew" solidblocks-cli:run --args="$*"
}

function task_publish() {
  export GITHUB_USERNAME="pellepelster"
  export GITHUB_TOKEN="$(pass solidblocks/github/personal_access_token_rw)"

  "${DIR}/gradlew" solidblocks-cloud-init:publish
}

#./do cli --db-password $(pass solidblocks/integration-test/db_password) --db-path $(pwd)/integration-test cloud config create --name blcks --domain blcks.de
#./do cli --db-password $(pass solidblocks/integration-test/db_password) --db-path $(pwd)/integration-test cloud config create-environment --name blcks --environment dev --hetzner-cloud-api-token $(pass solidblocks/integration-test/hcloud_api_token) --hetzner-dns-api-token $(pass solidblocks/integration-test/dns_api_token)

COMMAND="${1:-}"
shift || true

case ${COMMAND} in
  cli) task_cli "$@" ;;
  publish) task_publish "$@" ;;
esac
