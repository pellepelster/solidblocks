#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

CURL_WRAPPER_RETRY_DELAY=0.1

source "${DIR}/../test.sh"
source "${DIR}/../curl.sh"
source "${DIR}/../docker.sh"
source "${DIR}/../network.sh"

rm -rf "${DIR}/../.bin"
rm -rf "${DIR}/../.cache"

docker run --rm -d -p 8080 --name wiremock wiremock/wiremock:2.33.2

MAPPING1=$(cat <<-END
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
    "newScenarioState" : "download_redirect",
    "postServeActions" : [ ]
  }
END
)

MAPPING2=$(cat <<-END
{
    "name" : "download_redirect",
    "request" : {
      "url" : "/download-test",
      "method" : "GET"
    },
    "response" : {
      "status" : 301,
      "body" : "",
      "headers" : {
               "Location" : "/download-test-success"
       }
    },
    "persistent" : true,
    "priority" : 5,
    "scenarioName" : "retry-scenario",
    "requiredScenarioState" : "download_redirect",
    "newScenarioState" : "",
    "postServeActions" : [ ]
  }
END
)

MAPPING3=$(cat <<-END
{
    "name" : "download_success",
    "request" : {
      "url" : "/download-test-success",
      "method" : "GET"
    },
    "response" : {
      "status" : 200,
      "body" : "success\n",
      "headers" : { }
    },
    "persistent" : true,
    "priority" : 5,
    "postServeActions" : [ ]
  }
}
END
)


PORT=$(docker_mapped_tcp_port "wiremock" "8080")
network_wait_for_port "${PORT}"

curl_wrapper "http://localhost:${PORT}/__admin"

curl -X POST --silent --data "${MAPPING1}" "http://localhost:${PORT}/__admin/mappings" > /dev/null
curl -X POST --silent --data "${MAPPING2}" "http://localhost:${PORT}/__admin/mappings" > /dev/null
curl -X POST --silent --data "${MAPPING3}" "http://localhost:${PORT}/__admin/mappings" > /dev/null

test_assert_matches "curl_wrapper_success" "success" "$(curl_wrapper "http://localhost:${PORT}/download-test")"
test_assert_matches "curl_wrapper_invalid_host" "" "$(curl_wrapper "http://nonexistant-host")"
test_assert_matches "curl_wrapper_invalid_port" "" "$(curl_wrapper "http://localhost:12345")"
test_assert_matches "curl_wrapper_invalid_scheme" "" "$(curl_wrapper "https://localhost:${PORT}")"

trap "docker rm -f wiremock" EXIT ERR