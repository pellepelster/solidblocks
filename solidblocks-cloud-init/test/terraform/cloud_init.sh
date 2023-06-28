#!/usr/bin/env bash

set -eu -o pipefail

SOLIDBLOCKS_BASE_URL="${solidblocks_base_url}"
STORAGE_DEVICE="${storage_device}"

export SSL_PATH="/data1/ssl"
export SSL_EMAIL="contact@blcks.de"
export SSL_DOMAINS="test1.blcks.de test2.blcks.de"
export SSL_DNS_PROVIDER="hetzner"
export HETZNER_API_KEY="${hetzner_dns_api_token}"
export HETZNER_API_KEY="${hetzner_dns_api_token}"
export HETZNER_HTTP_TIMEOUT="30"

${cloud_minimal_skeleton}