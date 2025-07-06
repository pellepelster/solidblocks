function version_old() {
  if [[ "${CI:-}" == "true" ]]; then
    if [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
      echo "${GITHUB_REF_NAME:-}"
    else
      git rev-parse --short HEAD
    fi
  else
    echo "${VERSION:-snapshot}"
  fi
}

function current_version() {
  git describe --tags --abbrev=0 | sed -e "s/^v//"
}

function next_version() {
  local version="${1:-0.0.0}"
  local increment_type="${2:-patch}"
  local RE='[^0-9]*\([0-9]*\)[.]\([0-9]*\)[.]\([0-9]*\)\([0-9A-Za-z-]*\)'

  local MAJOR=$(echo $version | sed -e "s#$RE#\1#")
  local MINOR=$(echo $version | sed -e "s#$RE#\2#")
  local PATCH=$(echo $version | sed -e "s#$RE#\3#")

  case "$increment_type" in
  major)
    ((MAJOR += 1))
    ((MINOR = 0))
    ((PATCH = 0))
    ;;
  minor)
    ((MINOR += 1))
    ((PATCH = 0))
    ;;
  patch)
    ((PATCH += 1))
    ;;
  esac

  local NEXT_VERSION="$MAJOR.$MINOR.$PATCH"
  echo "$NEXT_VERSION"
}

function next_rc_version() {
  echo "$(next_version $(current_version))-rc"
}

function version() {
  if [[ "${CI:-}" == "true" ]] && [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
    echo "${GITHUB_REF_NAME:-}"
  else
    echo "$(next_rc_version)"
  fi
}
