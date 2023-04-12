#!/usr/bin/env bash

set -eu -o pipefail

export DEBIAN_FRONTEND="noninteractive"

apt-get update
#apt-get \
#        -o Dpkg::Options::="--force-confnew" \
#        --force-yes \
#        -fuy \
#        dist-upgrade

apt-get install --no-install-recommends -qq -y \
  nginx