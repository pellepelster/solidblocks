#!/usr/bin/env bash

TEMP_DIR="${DIR}/.test_$$"

function clean_temp_dir {
  rm -rf "${TEMP_DIR}"
  rm -rf "${DIR}/../../lib/.bin"
  rm -rf "${DIR}/../../lib/.cache"
}

clean_temp_dir
echo "creating temp dir '${TEMP_DIR}'"
mkdir -p "${TEMP_DIR}"
trap clean_temp_dir HUP INT QUIT TERM EXIT
