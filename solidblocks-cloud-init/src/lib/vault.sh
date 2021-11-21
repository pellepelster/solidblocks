#######################################
# vault.sh                            #
#######################################

function vault_read_secret() {
  local path="${1:-}"
  curl_wrapper -H "X-Vault-Token: ${VAULT_TOKEN}" "https://vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200/v1/${SOLIDBLOCKS_CLOUD}-${SOLIDBLOCKS_ENVIRONMENT}-kv/data/${path}" | jq .data.data
}

