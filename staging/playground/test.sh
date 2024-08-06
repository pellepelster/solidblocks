#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"


function test_stdout() {
  echo "test_stdout"
}

function test_stderr() {
  echo "test_stderr" 1>&2;
}

OUTPUT1="$(test_stdout)"
echo $OUTPUT1

OUTPUT2="$(test_stderr)"
echo $OUTPUT2