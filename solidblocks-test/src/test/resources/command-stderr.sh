#!/usr/bin/env bash

set -eu -o pipefail
DIR="$(cd "$(dirname "$0")" ; pwd -P)"


echo "stderr line 1" 1>&2
echo "stderr line 2" 1>&2
echo "stderr line 3" 1>&2