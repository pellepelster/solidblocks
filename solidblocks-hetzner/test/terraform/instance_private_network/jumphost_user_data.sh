#!/usr/bin/env bash

set -eu -o pipefail

apt-get update
apt-get install --no-install-recommends -qq -y postgresql-client
