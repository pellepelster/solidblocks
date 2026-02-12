#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )


# see https://pellepelster.github.io/solidblocks/shell/ci/#ci_detected
function ci_detected {

    # https://github.com/watson/ci-info/blob/master/index.js
    local variables="BUILD_ID BUILD_NUMBER CI CI_APP_ID CI_BUILD_ID CI_BUILD_NUMBER CI_NAME CONTINUOUS_INTEGRATION RUN_ID"
    for var in ${variables}; do
    if [[ -n "${!var:-}" ]]; then
      echo "true"
      return
    fi
    done

    # generated from https://raw.githubusercontent.com/watson/ci-info/master/vendors.json
    