#!/usr/bin/env bash
set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

(
  cd ${DIR}
  SOLIDCTL_CONFIG=$(cat ../test.json)
  CLOUD_INIT=$(cat  ../../build/cloud-init-controller_${VERSION}.sh)
  CLOUD_INIT="${CLOUD_INIT/@@SOLIDBLOCKS_CONFIG@@/$SOLIDCTL_CONFIG}"
  CLOUD_INIT="${CLOUD_INIT/@@ENVIRONMENT_VARIABLES@@/}"
  echo "${CLOUD_INIT}" > cloud-init-test-runner-controller/cloud-init.sh
  chmod +x cloud-init-test-runner-controller/cloud-init.sh

  docker-compose build cloud-init-test-runner-controller-0
  docker-compose build cloud-init-test-runner-controller-1
  docker-compose build cloud-init-test-runner-controller-2
)
