#!/usr/bin/env bash

set -eu
source ../../../lib/shell/utils.sh

HETZNER_CLOUD_API_URL=http://$(docker_aware_localhost):8081

function hcloud_api_mock_start() {
  docker run -d -p 8081:8080 --name hcloud-api-mock hcloud-api-mock
  while [[ "not mocked" != "$(curl --silent ${HETZNER_CLOUD_API_URL} | jq -r '.error')" ]]; do
    sleep 0.2
  done
}

function config() {
  echo "domain"
}

function hcloud_api_mock_stop() {
  docker rm --force "$(docker ps -f name=hcloud-api-mock -q)" > /dev/null
}
