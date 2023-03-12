#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

source "${DIR}/utils.sh"

#ls -lsa "${DIR}/../../examples/minimal-skeleton/do"


ls -lsa "${DIR}/../../build/solidblocks-shell-${VERSION}.zip"

mkdir -p "${TEMP_DIR}/pellepelster/solidblocks/releases/download/${VERSION}/"
cp "${DIR}/../../build/solidblocks-shell-${VERSION}.zip" "${TEMP_DIR}/pellepelster/solidblocks/releases/download/${VERSION}/"

docker run -it -d --rm --name nginx -p 8080:80 -v "${TEMP_DIR}:/usr/share/nginx/html" nginx

SOLIDBLOCKS_BASE_URL="http://localhost:8080" "${DIR}/../../examples/minimal-skeleton/do" bootstrap

function clean() {
  clean_temp_dir
  docker rm -f nginx
}

trap clean HUP INT QUIT TERM EXIT