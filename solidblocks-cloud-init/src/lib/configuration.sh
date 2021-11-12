#######################################
# configuration.sh                    #
#######################################

export DEBUG_LEVEL="${DEBUG_LEVEL:-0}"

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"

function bootstrap_solidblocks() {

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,instance,templates,config,lib,bin,certificates}
  chmod 700 "${SOLIDBLOCKS_DIR}/protected"

  local github_owner="pellepelster"
  (
      local config_file="${SOLIDBLOCKS_DIR}/cloud_init_config.json"
      vault_read_secret "nodes/cloud_init_config" > ${config_file}

      local temp_file="$(mktemp)"

      curl_wrapper -u "${github_owner}:$(jq -r ".github_token_ro" "${config_file}")" -L \
        https://maven.pkg.github.com/${github_owner}/solidblocks/solidblocks/solidblocks-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-cloud-init-${SOLIDBLOCKS_VERSION}.jar > ${temp_file}

      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip ${temp_file}
      rm -rf ${temp_file}
  )
}
