#######################################
# configuration.sh                    #
#######################################

export SOLIDBLOCKS_DEBUG_LEVEL="${SOLIDBLOCKS_DEBUG_LEVEL:-0}"

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"

function bootstrap_solidblocks() {

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chmod 700 "${SOLIDBLOCKS_DIR}/protected"

  echo "SOLIDBLOCKS_DEBUG_LEVEL=${SOLIDBLOCKS_DEBUG_LEVEL}" > "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_ENVIRONMENT=${SOLIDBLOCKS_ENVIRONMENT}" >> "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_CLOUD=${SOLIDBLOCKS_CLOUD}" >> "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_ROOT_DOMAIN=${SOLIDBLOCKS_ROOT_DOMAIN}" >> "/solidblocks/instance/environment"
  echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "/solidblocks/instance/environment"

  echo "VAULT_ADDR=${VAULT_ADDR}" >> "/solidblocks/instance/environment"
  echo "VAULT_TOKEN=${VAULT_TOKEN}" >> "/solidblocks/protected/environment"


  local github_owner="pellepelster"
  (
      local config_file="${SOLIDBLOCKS_DIR}/config/cloud_init_config.json"
      vault_read_secret "solidblocks/cloud/config" > ${config_file}

      local temp_file="$(mktemp)"

      #TODO verify checksum
      curl_wrapper -u "${github_owner}:$(jq -r ".github_token_ro" "${config_file}")" -L \
        https://maven.pkg.github.com/${github_owner}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar > ${temp_file}

      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip ${temp_file}
      rm -rf ${temp_file}
  )
}
