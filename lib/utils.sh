function version_current() {
  git describe --tags --abbrev=0
}

function version() {
  if [[ -z ${VERSION:-} ]]; then
    if [[ "${CI:-}" == "true" ]]; then
      if [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
        echo "${GITHUB_REF_NAME#v}"
      else
        echo "0.0.0"
      fi
    else
      echo "0.0.0"
    fi
  else
    echo "${VERSION#v}"
  fi
}

function terraform_replace_variable_default {
  local marker="${1:-}"
  local file="${2:-}"
  local version="${3:-}"

  mkdir -p ".tmp"
  rg -e "(^\s*default\s*=\s*\")(\S+)(\"\s*#${marker}\s*$)" --replace "\${1}${version}\${3}" --passthru --no-filename  --no-line-number --color never "${file}" > ".tmp/tmp.$$"
  mv ".tmp/tmp.$$" "${file}"
  rm -rf ".tmp"
}


function terraform_replace_module_version {
  local file="${1:-}"
  local version="${2:-}"

  mkdir -p ".tmp"
  cat "${file}" | rg 'v(\d+\.\d+\.\d+(?:-[a-zA-Z0-9-]+)?)' --passthru --no-filename  --no-line-number --color never --replace "${version}" > ".tmp/tmp.$$"
  mv ".tmp/tmp.$$" "${file}"
  rm -rf ".tmp"
}

function terraform_wrapper_clean {
  local module=${1:-}
  shift || true

  (
    cd "${module}"

    if [[ -d ".terraform" ]]; then
      rm -rf .terraform
    fi

    if [[ -f ".terraform.lock.hcl" ]]; then
      rm -rf ".terraform.lock.hcl"
    fi

    if [[ -f ".terraform.lock.hcl" ]]; then
      rm -rf ".terraform.lock.hcl"
    fi
  )
}

function terraform_wrapper {
  local module=${1:-}
  shift || true

  (
    cd "${module}"

    if [[ ! -d ".terraform" ]]; then
      terraform init -upgrade
    fi

    export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"

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

function documentation_prepare_files {
    local snippet_dir="${CONFIG_ROOT}/doc/content/snippets"
    rm -rf "${snippet_dir}"
    mkdir -p "${snippet_dir}"

    if [[ -n "${CI:-}" ]]; then
      rsync -rv --exclude-from ${CONFIG_ROOT}/rsync_exclude ${CONFIG_ROOT}/*/snippets/* "${snippet_dir}"
    else
      rsync -rv --exclude-from ${CONFIG_ROOT}/rsync_exclude ${CONFIG_ROOT}/*/snippets/* "${snippet_dir}"
      rsync -rv --exclude-from ${CONFIG_ROOT}/rsync_exclude ${CONFIG_ROOT}/*/build/snippets/* "${snippet_dir}"
    fi
}

function documentation_prepare_env {
  local versions="$(grep  'VERSION=\".*\"' "${CONFIG_ROOT}/solidblocks-shell/lib/software.sh")"
  for version in ${versions}; do
    eval "export ${version}"
  done
  export SOLIDBLOCKS_VERSION="$VERSION"
  export SOLIDBLOCKS_VERSION_RAW="${VERSION#"v"}"
}