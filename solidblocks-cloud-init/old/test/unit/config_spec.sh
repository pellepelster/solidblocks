#!/usr/bin/env bash

set -eu

SOLIDBLOCKS_DIR="/tmp/$(uuidgen)"
SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
mkdir -p "${SOLIDBLOCKS_DIR}"
cp "../test.json" "${SOLIDBLOCKS_CONFIG_FILE}"

source ../../lib/config.sh
source ../../../lib/shell/test.sh

assert_equals 'WNJLRLHVSrAE7fDqozqd6w==' "$(config '.consul_secret')" "config: consul_secret"

HETZNER_CLOUD_API_TOKEN="${HETZNER_CLOUD_API_TOKEN:-$(config '.hetzner_cloud_api_token')}"
assert_equals 'YKKZ2PdUbnUZvlaLAHXM5f0XXJqtqzP5uaIrQlwgVNPkD3GEruB93O2Vs3QhCdol' "${HETZNER_CLOUD_API_TOKEN}" "config: hetzner_cloud_api_token"
