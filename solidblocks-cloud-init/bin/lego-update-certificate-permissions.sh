#!/usr/bin/env bash

set -eu

DIR="$(
  cd "$(dirname "$0")"
  pwd -P
)"

chgrp solidblocks "${LEGO_CERT_PATH}"
chmod g+r "${LEGO_CERT_PATH}"

chgrp solidblocks "${LEGO_CERT_KEY_PATH}"
chmod g+r "${LEGO_CERT_KEY_PATH}"