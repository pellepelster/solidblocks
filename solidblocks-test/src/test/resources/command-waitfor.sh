#!/usr/bin/env bash

set -eu -o pipefail
DIR="$(cd "$(dirname "$0")" ; pwd -P)"

sleep 1
echo "marker 1"

sleep 2
echo "marker 2"

sleep 3
echo "marker 3"
