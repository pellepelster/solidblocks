#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

source "${DIR}/../test.sh"
source "${DIR}/../curl.sh"
source "${DIR}/../docker.sh"
source "${DIR}/../network.sh"

docker run --rm -d -p 8080 --name wiremock wiremock/wiremock:2.33.2

MAPPING1=$(cat <<-END
{
    "name" : "download_success",
    "request" : {
      "url" : "/download-test",
      "method" : "GET"
    },
    "response" : {
      "status" : 200,
      "body" : "success\n",
      "headers" : { }
    },
    "persistent" : true,
    "priority" : 5,
    "scenarioName" : "retry-scenario",
    "requiredScenarioState" : "first_download_failed",
    "postServeActions" : [ ]
  }
}
END
)

MAPPING2=$(cat <<-END
{
    "name" : "download_error_500",
    "request" : {
      "url" : "/download-test",
      "method" : "GET"
    },
    "response" : {
      "status" : 500,
      "body" : "",
      "headers" : { }
    },
    "persistent" : true,
    "priority" : 5,
    "scenarioName" : "retry-scenario",
    "requiredScenarioState" : "Started",
    "newScenarioState" : "first_download_failed",
    "postServeActions" : [ ]
  }
END
)

PORT=$(docker_mapped_tcp_port "wiremock" "8080")
network_wait_for_port "${PORT}"

curl_wrapper "http://localhost:${PORT}/__admin"

curl -X POST --silent --data "${MAPPING1}" "http://localhost:${PORT}/__admin/mappings" > /dev/null
curl -X POST --silent --data "${MAPPING2}" "http://localhost:${PORT}/__admin/mappings" > /dev/null

test_assert_matches "curl_wrapper" "success" "$(curl_wrapper "http://localhost:${PORT}/download-test")"

trap "docker rm -f wiremock" EXIT ERR