#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/docker.sh"
source "${DIR}/utils.sh"

NETWORK="$(uuidgen)"

docker_ensure_network "${NETWORK}"
docker_ensure_network "${NETWORK}"

docker_remove_network "${NETWORK}"