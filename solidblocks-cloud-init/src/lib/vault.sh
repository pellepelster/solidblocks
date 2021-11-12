#######################################
# vault.sh                            #
#######################################

function vault_read_secret() {
  local path="${1:-}"
  curl_wrapper -H "X-Vault-Token: ${SOLIDBLOCKS_VAULT_TOKEN}" "https://vault.${SOLIDBLOCKS_ENVIRONMENT}.${SOLIDBLOCKS_ROOT_DOMAIN}:8200/v1/solidblocks/data/${path}" | jq .data.data
}

