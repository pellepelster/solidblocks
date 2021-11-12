#######################################
# config.sh                           #
#######################################

function config() {
    local path=${1:-}
    jq -r "${path}" "${SOLIDBLOCKS_CONFIG_FILE}"
}
