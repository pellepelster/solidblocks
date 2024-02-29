#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

"${DIR}/single-project-bash/do" clean
"${DIR}/single-project-bash/do" build || true
"${DIR}/single-project-bash/do" build debug
"${DIR}/single-project-bash/do" build-documentation
"${DIR}/single-project-bash/do" test
"${DIR}/single-project-bash/do" version
"${DIR}/single-project-bash/do" deploy
"${DIR}/single-project-bash/do" || true
"${DIR}/single-project-bash/do" clean
