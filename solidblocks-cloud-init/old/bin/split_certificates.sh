#!/bin/bash

set -eux

source /solidblocks/instance/environment

SYSTEM_CA_FILE="/usr/local/share/ca-certificates/${CLOUD_NAME}.crt"
SOLIDBLOCKS_CERTIFICATES_DIR="/solidblocks/certificates"

cat "${SOLIDBLOCKS_CERTIFICATES_DIR}/certificates.json" | jq -r .certificate | base64 -d > "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).cert.pem"
cat "${SOLIDBLOCKS_CERTIFICATES_DIR}/certificates.json" | jq -r .private_key | base64 -d > "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem"
chown root:solidblocks "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem"
chmod 750 "${SOLIDBLOCKS_CERTIFICATES_DIR}/$(hostname).key.pem"

cat "${SOLIDBLOCKS_CERTIFICATES_DIR}/certificates.json" | jq -r .issuing_ca | base64 -d > "${SYSTEM_CA_FILE}"

if [[ ! -f "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem" ]]; then
    ln -s "${SYSTEM_CA_FILE}" "${SOLIDBLOCKS_CERTIFICATES_DIR}/ca.cert.pem"
fi

update-ca-certificates --fresh
