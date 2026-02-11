#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
source ${DIR}/utils.sh
VERSION=$(version)
echo ${VERSION#v}