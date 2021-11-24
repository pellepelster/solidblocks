#######################################
# vault.sh                            #
#######################################

function vault_read_secret() {
  local path="${1:-}"
  curl_wrapper -H "X-Vault-Token: ${VAULT_TOKEN}" "${VAULT_ADDR}/v1/${SOLIDBLOCKS_CLOUD}-${SOLIDBLOCKS_ENVIRONMENT}-kv/data/${path}" | jq .data.data
}

