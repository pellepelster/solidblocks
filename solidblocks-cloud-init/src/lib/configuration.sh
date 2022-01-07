#######################################
# configuration.sh                    #
#######################################

export SOLIDBLOCKS_DEBUG_LEVEL="${SOLIDBLOCKS_DEBUG_LEVEL:-0}"

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"
export SOLIDBLOCKS_STORAGE_LOCAL_DIR="/storage/local"

function bootstrap_solidblocks() {

  groupadd solidblocks

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chmod 770 ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chgrp solidblocks ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}

  echo "SOLIDBLOCKS_DEBUG_LEVEL=${SOLIDBLOCKS_DEBUG_LEVEL}" > "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_ENVIRONMENT=${SOLIDBLOCKS_ENVIRONMENT}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_HOSTNAME=${SOLIDBLOCKS_HOSTNAME}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_CLOUD=${SOLIDBLOCKS_CLOUD}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_ROOT_DOMAIN=${SOLIDBLOCKS_ROOT_DOMAIN}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

  echo "VAULT_ADDR=${VAULT_ADDR}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

  echo "VAULT_TOKEN=${VAULT_TOKEN}" > "${SOLIDBLOCKS_DIR}/protected/environment"
  echo "GITHUB_TOKEN_RO=$(vault_read_secret "solidblocks/cloud/providers/github" | jq -r '.github_token_ro')" >> "${SOLIDBLOCKS_DIR}/protected/environment"
  echo "GITHUB_USERNAME=$(vault_read_secret "solidblocks/cloud/providers/github" | jq -r '.github_username')" >> "${SOLIDBLOCKS_DIR}/protected/environment"

  export $(xargs < "${SOLIDBLOCKS_DIR}/protected/environment")
  (
      local temp_file="$(mktemp)"

      #TODO verify checksum
      curl_wrapper -u "${GITHUB_USERNAME}:${GITHUB_TOKEN_RO}" -L \
        "https://maven.pkg.github.com/${GITHUB_USERNAME}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar" > "${temp_file}"

      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip "${temp_file}"
      rm -rf "${temp_file}"
  )
}
