#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
SOLIDBLOCKS_BOOTSTRAP_ADDRESS="${SOLIDBLOCKS_BOOTSTRAP_ADDRESS:-https://maven.pkg.github.com}"

export $(xargs < "${SOLIDBLOCKS_DIR}/instance/environment")
export $(xargs < "${SOLIDBLOCKS_DIR}/protected/environment")
export $(xargs < "${SOLIDBLOCKS_DIR}/service/environment")

echo "bootstrapping '${SOLIDBLOCKS_COMPONENT}' from '${SOLIDBLOCKS_BOOTSTRAP_ADDRESS}'"

COMPONENT_ACTIVE="${SOLIDBLOCKS_DIR}/service/${SOLIDBLOCKS_COMPONENT}-active"
DOWNLOAD_DIR="${SOLIDBLOCKS_DIR}/download"
mkdir -p "${DOWNLOAD_DIR}"

if [[ -f "${COMPONENT_ACTIVE}/update.version" ]]; then
  COMPONENT_VERSION="$(cat "${COMPONENT_ACTIVE}/update.version")"
  echo "updating component to '${COMPONENT_VERSION}'"
else
  COMPONENT_VERSION="${SOLIDBLOCKS_VERSION}"
  echo "downloading initial version '${COMPONENT_VERSION}'"
fi

COMPONENT_URL="${SOLIDBLOCKS_BOOTSTRAP_ADDRESS}/pellepelster/solidblocks/solidblocks/${SOLIDBLOCKS_COMPONENT}/${COMPONENT_VERSION}/${SOLIDBLOCKS_COMPONENT}-${COMPONENT_VERSION}.tar"
COMPONENT_NAME="${SOLIDBLOCKS_COMPONENT}-${COMPONENT_VERSION}"
COMPONENT_DISTRIBUTION="${DOWNLOAD_DIR}/${COMPONENT_NAME}.tar"

DOWNLOAD_OPTIONS=""
SKIP_DOWNLOAD=0

if [ -L "${COMPONENT_ACTIVE}" ] && [ -e "${COMPONENT_ACTIVE}" ]; then
  if curl --silent --location --show-error --fail -o /dev/null "${COMPONENT_URL}"; then
    echo "download url '${COMPONENT_URL}' exists and active component found, trying download with fallback to active version"
   DOWNLOAD_OPTIONS="--fail"
  else
    echo "download url '${COMPONENT_URL}' does not exist, falling back to active component"
    SKIP_DOWNLOAD=1
  fi
fi

if [[ ${SKIP_DOWNLOAD} -eq 0 ]]; then
  echo "downloading '${COMPONENT_URL}' to '${COMPONENT_DISTRIBUTION}'"
  echo "------------------------------------------------------------------------------------------"
  while ! curl ${DOWNLOAD_OPTIONS} --retry 25 --retry-connrefused --location --show-error "${COMPONENT_URL}" > "${COMPONENT_DISTRIBUTION}"; do
      echo "download failed, retrying"
      sleep 10
  done
  echo "------------------------------------------------------------------------------------------"
  printf "\n\n"

  (
    cd "${SOLIDBLOCKS_DIR}/service"
    echo "extracting '${COMPONENT_DISTRIBUTION}' to '$(pwd)'"
    echo "------------------------------------------------------------------------------------------"
    tar -xvf "${COMPONENT_DISTRIBUTION}"
    echo "------------------------------------------------------------------------------------------"
    printf "\n\n"
  )

  rm -f "${COMPONENT_ACTIVE}"
  ln -s "${COMPONENT_NAME}" "${COMPONENT_ACTIVE}"

fi


cd "${COMPONENT_ACTIVE}"
exec "${COMPONENT_ACTIVE}/bin/${SOLIDBLOCKS_COMPONENT}"