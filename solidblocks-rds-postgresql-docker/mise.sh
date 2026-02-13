#!/usr/bin/env bash

export DOCKER_API_VERSION=1.52

function task_build {
  local versions_to_build=${1:-$POSTGRES_VERSIONS}

  (
    mkdir -p "build"

    if [[ "${BUILD_FAST:-}" == "true" ]]; then
      cp "install/fast-locale.gen" "${TEMP_DIR}/locale.gen"
    else
      cp "install/all-locale.gen" "${TEMP_DIR}/locale.gen"
    fi

    for postgres_version in ${versions_to_build}; do
      local postgres_previous_version=$((postgres_version-1))
      local postgres_previous_version_path=${postgres_previous_version}

      if [[ ${postgres_previous_version} -gt 13 ]]; then
        previous_version_pgaudit_branch="REL_${postgres_previous_version}_STABLE"
      else
        previous_version_pgaudit_branch="unsupported/REL_${postgres_previous_version}_STABLE"
      fi

      echo "========================================================================"
      echo "version: ${VERSION}"
      echo "postgres_version: ${postgres_version}"
      echo "postgres_previous_version: ${postgres_previous_version}"
      echo "previous_version_pgaudit_branch: ${previous_version_pgaudit_branch}"
      echo "docker platform: ${DOCKER_PLATFORM}"
      echo "docker options: ${DOCKER_OPTIONS}"
      echo "docker tags:"
      echo "  - ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:${postgres_version}-${VERSION}-snapshot"
      echo "  - ${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:${postgres_version}-snapshot"
      echo "========================================================================"

      docker buildx build \
        --platform ${DOCKER_PLATFORM} \
        ${DOCKER_OPTIONS} \
        --cache-to=type=local,dest=${CACHE_DIR} \
        --cache-from=type=local,src=${CACHE_DIR} \
        --build-arg POSTGRES_VERSION=${postgres_version} \
        --build-arg POSTGRES_PREVIOUS_VERSION=${postgres_previous_version} \
        --build-arg PREVIOUS_VERSION_PGAUDIT_BRANCH=${previous_version_pgaudit_branch} \
        --tag "${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:${postgres_version}-${VERSION}-snapshot" \
        --tag "${DOCKER_REGISTRY}/${DOCKER_REPOSITORY}/${DOCKER_IMAGE_NAME}:${postgres_version}-snapshot" \
        .
    done
  )
}

function task_test {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  mkdir -p "${TEMP_DIR}"

  (
    cd "terraform"
    terraform init
    terraform apply --auto-approve
  )
  (
    export GCP_SERVICE_ACCOUNT_KEY="${GCP_SERVICE_ACCOUNT_KEY:-$(pass solidblocks/gcp/test/service_account_key)}"
    echo "${GCP_SERVICE_ACCOUNT_KEY}" > "${TEMP_DIR}/service-key.json"
    export GOOGLE_APPLICATION_CREDENTIALS="${TEMP_DIR}/service-key.json"
    export DB_BACKUP_GCS_SERVICE_KEY_BASE64="$(cat "${TEMP_DIR}/service-key.json" | base64 -w0)"

    ../gradlew :solidblocks-rds-postgresql:test $@
  )
}