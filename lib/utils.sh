function current_version() {
  git describe --tags --abbrev=0
}

function version() {
  if [[ -z ${VERSION:-} ]]; then
    if [[ "${CI:-}" == "true" ]]; then
      if [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
        echo "${GITHUB_REF_NAME:-}"
      else
        echo "v0.0.0-dev"
      fi
    else
      echo "$(current_version)-dev"
    fi
  else
    echo "${VERSION}"
  fi
}

function version_rc() {
  echo "$(version)-rc"
}
