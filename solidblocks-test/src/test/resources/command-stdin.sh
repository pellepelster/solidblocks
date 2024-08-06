#!/usr/bin/env bash

set -eu -o pipefail
DIR="$(cd "$(dirname "$0")" ; pwd -P)"

sleep 2
echo "marker 1"
read
echo "read 1 received"

sleep 2
echo "marker 2"
read
echo "read 2 received"

sleep 2
echo "marker 3"
read
echo "read 3 received"
