function version_current() {
  git describe --tags --abbrev=0
}

function version() {
  if [[ -z ${VERSION:-} ]]; then
    if [[ "${CI:-}" == "true" ]]; then
      if [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
        echo "${GITHUB_REF_NAME:-}"
      else
        echo "v0.0.0"
      fi
    else
      echo "v0.0.0"
      #echo "$(version_current)"
    fi
  else
    echo "${VERSION}"
  fi
}

function version_ensure() {
    local version="${1:-}"

    if [[ -z "${version}" ]]; then
      echo "no version set"
      exit 1
    fi

    if [[ "${version}" =~ ^v[0-9]{1,2}\.[0-9]{1,2}\.[0-9]{1,2}(-rc[0-9]{1,2})?$ ]]; then
      echo "version: '${version}'"
    else
      echo "invalid version '${version}'"
      exit 1
    fi
}

function terraform_replace_module_version {
  local file="${1:-}"
  local version="${2:-}"

  mkdir -p "${DIR}/.tmp"

  cat "${file}" | rg 'v(\d+\.\d+\.\d+(?:-[a-zA-Z0-9-]+)?)' --passthru --no-filename  --no-line-number --color never --replace "${version}" > "${DIR}/.tmp/tmp.$$"
  mv "${DIR}/.tmp/tmp.$$" "${file}"
  rm -rf "${DIR}/.tmp"
}

function terraform_wrapper {
  local module=${1:-}
  shift || true

  (
    cd "${DIR}/${module}"

    if [[ ! -d ".terraform" ]]; then
      terraform init -upgrade
    fi

    export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"
    export HETZNER_DNS_API_TOKEN="${HETZNER_DNS_API_TOKEN:-$(pass solidblocks/hetzner/test/dns_api_token)}"

    export TF_VAR_hetzner_dns_api_token="${HETZNER_DNS_API_TOKEN}"

    export TF_VAR_hetzner_s3_access_key="${SOLIDBLOCKS_HETZNER_TEST_S3_ACCESS_KEY_ID:-$(pass solidblocks/hetzner/test/s3_access_key_id)}"
    export TF_VAR_hetzner_s3_secret_key="${SOLIDBLOCKS_HETZNER_TEST_S3_SECRET_KEY:-$(pass solidblocks/hetzner/test/s3_secret_key)}"

    export AWS_DEFAULT_REGION="eu-central-1"
    export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/test/access_key_id)}" #
    export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/test/secret_access_key)}"

    export TF_VAR_backup_s3_access_key="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/test/access_key_id)}" #
    export TF_VAR_backup_s3_secret_key="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/test/secret_access_key)}"
    export TF_VAR_solidblocks_version=${VERSION}

    terraform ${@}
  )
}
