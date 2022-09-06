#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
TEMP_DIR="${DIR}/.tmp"

rm -rf "${DIR}/../../lib/.bin"
rm -rf "${DIR}/../../lib/.cache"

source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/docker.sh"

NETWORK="$(uuidgen)"

docker_ensure_network "${NETWORK}"
docker_ensure_network "${NETWORK}"

docker_remove_network "${NETWORK}"