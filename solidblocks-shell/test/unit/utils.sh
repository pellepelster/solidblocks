#!/usr/bin/env bash

TEMP_DIR="${DIR}/.test_$$"

function clean_temp_dir {
  rm -rf "${TEMP_DIR}"
  rm -rf "${DIR}/../../lib/.bin"
  rm -rf "${DIR}/../../lib/.cache"
}

clean_temp_dir
mkdir -p "${TEMP_DIR}"
trap clean_temp_dir HUP INT QUIT TERM EXIT
