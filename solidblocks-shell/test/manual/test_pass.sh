#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

SECRETS_DIR="${DIR}/test_secrets"

source "${DIR}/../../lib/pass.sh"
source "${DIR}/../../lib/test.sh"


secret="$(uuidgen)"

pass_ensure_initialized
pass_wrapper
echo "${secret}" | pass_wrapper insert --echo --force "abc/def/hij"
pass_wrapper
test_assert_matches "get secret" "${secret}" "$(pass_secret "abc/def/hij")"

export ABC_DEF_HIJ="xxx"
test_assert_matches "get secret from environment" "xxx" "$(pass_secret "abc/def/hij")"
