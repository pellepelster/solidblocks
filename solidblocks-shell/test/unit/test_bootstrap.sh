#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

source "${DIR}/utils.sh"
source "${DIR}/../../lib/docker.sh"


ls -lsa "${DIR}/../../build/solidblocks-shell-${VERSION}.zip"

mkdir -p "${TEMP_DIR}/pellepelster/solidblocks/releases/download/${VERSION}/"
cp "${DIR}/../../build/solidblocks-shell-${VERSION}.zip" "${TEMP_DIR}/pellepelster/solidblocks/releases/download/${VERSION}/"

TEST_ID="bootstrap_$(uuidgen)"

docker run -it -d --rm --name "${TEST_ID}" -p 80 -v "${TEMP_DIR}:/usr/share/nginx/html" nginx

sleep 2

SOLIDBLOCKS_BASE_URL="http://localhost:$(docker_mapped_tcp_port "${TEST_ID}" "80")" "${DIR}/../../build/documentation/shell_minimal_skeleton_do" bootstrap

function clean() {
  clean_temp_dir
  docker rm -f "${TEST_ID}"
}

trap clean HUP INT QUIT TERM EXIT