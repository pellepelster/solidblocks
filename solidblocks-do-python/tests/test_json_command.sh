#!/usr/bin/env bash

set -eu

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../lib/utils.sh"
export VERSION="$(version)"

TEMP_DIR="${DIR}/.temp_$$"

function clean_temp_dir {
  rm -rf "${TEMP_DIR}"
}

trap clean_temp_dir EXIT

source "${DIR}/../solidblocks-shell/lib/log.sh"
source "${DIR}/../solidblocks-shell/lib/python.sh"

function task_test() {
  export FOO="foo_env"
  export TEST_SOME_PASSWORD="test_some_password_env"
  export TEST_SOME_SECRET_WITH_DASHES="test_some_secret_with_dashes_env"
  export ONLY_FROM_ENV="only_from_env"
  poetry run pytest -s
}

function task_build() {
    if [[ ! "${VERSION}" =~ "v[0-9]*\.[0-9]*\.[0-9]*" ]]; then
      poetry version "0.0.0-pre"
    else
      poetry version "${VERSION}"
    fi
    poetry install
    poetry build
}

function task_usage {
  echo "Usage: $0 ..."
  exit 1
}

arg=${1:-}
shift || true
case ${arg} in
  build) task_build "$@" ;;
  test) task_test "$@" ;;
  format) task_format "$@" ;;
  *) task_usage ;;
esac
