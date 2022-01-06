#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
SOLIDBLOCKS_BOOTSTRAP_ADDRESS="${SOLIDBLOCKS_BOOTSTRAP_ADDRESS:-https://maven.pkg.github.com}"

export $(xargs < "${SOLIDBLOCKS_DIR}/instance/environment")
export $(xargs < "${SOLIDBLOCKS_DIR}/protected/initial_environment")
export $(xargs < "${SOLIDBLOCKS_DIR}/service/environment")

echo "bootstrapping '${SOLIDBLOCKS_COMPONENT}' from '${SOLIDBLOCKS_BOOTSTRAP_ADDRESS}'"

DOWNLOAD_DIR="${SOLIDBLOCKS_DIR}/download"
mkdir -p "${DOWNLOAD_DIR}"

COMPONENT_URL="${SOLIDBLOCKS_BOOTSTRAP_ADDRESS}/pellepelster/solidblocks/solidblocks/${SOLIDBLOCKS_COMPONENT}/${SOLIDBLOCKS_VERSION}/${SOLIDBLOCKS_COMPONENT}-${SOLIDBLOCKS_VERSION}.tar"
COMPONENT_NAME="${SOLIDBLOCKS_COMPONENT}-${SOLIDBLOCKS_VERSION}"
COMPONENT_DISTRIBUTION="${DOWNLOAD_DIR}/${COMPONENT_NAME}.tar"

echo "downloading '${COMPONENT_URL}' to '${COMPONENT_DISTRIBUTION}'"

while ! curl --retry 25 --retry-connrefused --silent --location --show-error "${COMPONENT_URL}" > "${COMPONENT_DISTRIBUTION}"; do
    echo "download failed, retrying"
    sleep 10
done

SOLIDBLOCKS_COMPONENT_ACTIVE="${SOLIDBLOCKS_DIR}/service/${SOLIDBLOCKS_COMPONENT}-active"

(
  cd "${SOLIDBLOCKS_DIR}/service"
  echo "extracting '${COMPONENT_DISTRIBUTION}' to '$(pwd)'"
  tar -xvf "${COMPONENT_DISTRIBUTION}"
)

rm -f "${SOLIDBLOCKS_COMPONENT_ACTIVE}"
ln -s "${COMPONENT_NAME}" "${SOLIDBLOCKS_COMPONENT_ACTIVE}"

cd "${SOLIDBLOCKS_COMPONENT_ACTIVE}"
exec "${SOLIDBLOCKS_COMPONENT_ACTIVE}/bin/${SOLIDBLOCKS_COMPONENT}"