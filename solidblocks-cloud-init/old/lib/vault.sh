function vault() {
  curl --silent --header "X-Vault-Token: s.6U2gxICJUj8wDDZfWmIlXMYq" https://vault.dev.blcks.de:8200/v1/solidblocks/data/cloud_init | jq .data.data
}
