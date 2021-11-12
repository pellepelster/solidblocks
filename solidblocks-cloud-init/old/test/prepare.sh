#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

(
  cd ${DIR}

  cp ../../lib/spec/hcloud_api_mock.rb hcloud-api-mock/hcloud_api_mock_generated.rb
  cd hcloud-api-mock
  docker rm -f hcloud-api-mock || true
  docker build -t hcloud-api-mock .
)
