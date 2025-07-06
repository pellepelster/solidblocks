function current_version() {
  git describe --tags --abbrev=0
}

function version() {
  if [[ "${CI:-}" == "true" ]] && [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
    echo "${GITHUB_REF_NAME:-}"
  else
    echo "$(current_version)-dev"
  fi
}

function version_rc() {
  if [[ "${CI:-}" == "true" ]] && [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
    echo "${GITHUB_REF_NAME:-}-rc"
  else
    echo "$(current_version)-dev-rc"
  fi
}
