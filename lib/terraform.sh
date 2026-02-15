function terraform_export_credentials() {
  export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"

  export AWS_DEFAULT_REGION="eu-central-1"
  export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/test/access_key_id)}" #
  export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/test/secret_access_key)}"
}

function terraform_wrapper() {
  local dir="${1:-}"
  shift || true
  (
    terraform_export_credentials

    cd "${dir}"

    terraform init -upgrade

    terraform $@
  )
}

function terraform_output() {
  local dir="${1:-}"
  local name="${2:-}"
  shift || true
  shift || true
  terraform_wrapper "${dir}" output -raw "${name}"
}
