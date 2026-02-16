#!/usr/bin/env bash
set -eu -o pipefail

function version_ensure() {
    local version="${1:-}"

    if [[ -z "${version}" ]]; then
      echo "no version set"
      exit 1
    fi

    if [[ "${version}" =~ ^[0-9]{1,2}\.[0-9]{1,2}\.[0-9]{1,2}(-rc[0-9]{1,2})?$ ]]; then
      echo "version: '${version}'"
    else
      echo "invalid version '${version}'"
      exit 1
    fi
}

function task_release_check() {
  local version="${1:-}"
  version_ensure "${version}"

  local previous_tag="$(git --no-pager tag | sed '/-/!{s/$/_/}' | sort -V | sed 's/_$//' | tail -1)"
  local previous_version="${previous_tag#v}"

  echo "previous version: '${previous_version}'"

  local previous_version_escaped="${previous_version//\./\\.}"

  if [[ "${version}" == *"pre"* ]]; then
    echo "skipping check for pre-version '${version}'"
  else
    echo "checking for previous version '${previous_version}'"
    if git --no-pager grep "${previous_version_escaped}" | grep -v "poetry.lock" | grep -v CHANGELOG.md | grep -v "doc/content/runbooks/paperback" | grep -v README.md; then
      echo "previous version '${previous_version_escaped}' found in repository"
      exit 1
    fi
  fi

  if [[ "${version}" == *"rc"* ]]; then
    echo "skipping changelog check for pre-version '${version}'"
  else
    echo "checking changelog for current version '${version}'"
    if ! grep "${version}" "CHANGELOG.md"; then
      echo "version '${version}' not found in changelog"
      exit 1
    fi
  fi

  if [[ $(git diff --stat) != '' ]]; then
    echo "repository is dirty"
    exit 1
  fi
}

function task_release {
  local version="${1:-}"
  version_ensure "${version}"

  git tag -a "v${version}" -m "v${version}"
  git push --tags
}

function task_clean_aws {
  aws-nuke run \
    --access-key-id "$(pass solidblocks/aws/admin/access_key_id)" \
    --secret-access-key "$(pass solidblocks/aws/admin/secret_access_key)" \
    --config contrib/aws-nuke.yaml \
    --no-dry-run \
    --force
}

