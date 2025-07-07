function current_version() {
  if which git >/dev/null 2>&1; then
    git describe --tags --abbrev=0
  else
    echo "0.0.0"
  fi
}

function version() {
  if [[ -z ${VERSION:-} ]]; then
    if [[ "${CI:-}" == "true" ]] && [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
      echo "${GITHUB_REF_NAME:-}"
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
