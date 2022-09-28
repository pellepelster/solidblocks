#!/usr/bin/env bash

set -eu

DIR="$(cd "$(dirname "$0")" ; pwd -P)"


DB_ADMIN_USERNAME="${USER}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:-$(uuidgen)}"

echo $DB_ADMIN_PASSWORD