function terraform_export_credentials() {
  export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"
  export HETZNER_DNS_API_TOKEN="${HETZNER_DNS_API_TOKEN:-$(pass solidblocks/hetzner/test/dns_api_token)}"

  export AWS_DEFAULT_REGION="eu-central-1"
  export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/test/secret_access_key)}"
  export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/test/access_key)}" #
}

function terraform_wrapper() {
  local dir="${1:-}"
  shift || true
  (
    terraform_export_credentials

    cd "${dir}"

    #if [[ ! -d ".terraform" ]]; then
      terraform init -upgrade
    #fi

    terraform $@
  )
}
