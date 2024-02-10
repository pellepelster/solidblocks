#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/ci.sh"

export GITHUB_ACTIONS=${GITHUB_ACTIONS:-something}
test_assert_matches "ci_detected_true" "true" "$(ci_detected)"

if [[ $(ci_detected) == "true" ]]; then
  echo "we are running in CI"
else
  echo "we are running somewhere else"
fi

unset GITHUB_ACTIONS
test_assert_matches "ci_detected_false" "false" "$(ci_detected)"

export CI_NAME="codeship"
test_assert_matches "ci_detected_codeship" "true" "$(ci_detected)"
unset CI_NAME
test_assert_matches "ci_detected_false" "false" "$(ci_detected)"

export JENKINS_URL="yolo"
export BUILD_ID="yolo"
test_assert_matches "ci_detected_jenkins" "true" "$(ci_detected)"
unset JENKINS_URL
unset BUILD_ID
test_assert_matches "ci_detected_false" "false" "$(ci_detected)"

export NODE="prefix /app/.heroku/node/bin/node postfix"
test_assert_matches "ci_detected_node" "true" "$(ci_detected)"
unset NODE
test_assert_matches "ci_detected_false" "false" "$(ci_detected)"

if [[ $(ci_detected) == "true" ]]; then
  echo "we are running in CI"
else
  echo "we are running somewhere else"
fi

