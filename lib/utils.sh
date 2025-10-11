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
      echo "$(version_current)"
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