#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )


# see https://pellepelster.github.io/solidblocks/shell/ci/#ci_detected
function ci_detected {

    # https://github.com/watson/ci-info/blob/master/index.js
    local variables="BUILD_ID BUILD_NUMBER CI CI_APP_ID CI_BUILD_ID CI_BUILD_NUMBER CI_NAME CONTINUOUS_INTEGRATION RUN_ID"
    for var in ${variables}; do
    if [[ -n "${!var:-}" ]]; then
      echo "true"
      return
    fi
    done

    # generated from https://raw.githubusercontent.com/watson/ci-info/master/vendors.json
    
    if [[ -n "${AGOLA_GIT_REF:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${AC_APPCIRCLE:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${APPVEYOR:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${CODEBUILD_BUILD_ARN:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${TF_BUILD:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${bamboo_planKey:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${BITBUCKET_COMMIT:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${BITRISE_IO:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${BUDDY_WORKSPACE_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${BUILDKITE:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${CIRCLECI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${CIRRUS_CI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${CF_PAGES:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${WORKERS_CI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${CF_BUILD_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${CM_BUILD_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    
    
    if [[ "${CI_NAME:-}" == "codeship" ]]; then
      echo "true"
      return
      
    fi
    
    
    
    if [[ -n "${DRONE:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${DSARI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${EARTHLY_CI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${EAS_BUILD:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${GERRIT_PROJECT:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${GITEA_ACTIONS:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${GITLAB_CI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${GO_PIPELINE_LABEL:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${BUILDER_OUTPUT:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${HARNESS_BUILD_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    
    if [[ "${NODE:-}" == *"/app/.heroku/node/bin/node"* ]]; then
      echo "true"
      return
    fi
    
    
    
    if [[ -n "${HUDSON_URL:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${JENKINS_URL:-}" ]] && [[ -n "${BUILD_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${LAYERCI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${MAGNUM:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${NETLIFY:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${NEVERCODE:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${PROW_JOB_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${RELEASE_BUILD_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${RENDER:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${SAILCI:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${SCREWDRIVER:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${SEMAPHORE:-}" ]]; then
      echo "true"
      return
    fi
    
    
    
    
    if [[ "${CI_NAME:-}" == "sourcehut" ]]; then
      echo "true"
      return
      
    fi
    
    
    
    if [[ -n "${STRIDER:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${TASK_ID:-}" ]] && [[ -n "${RUN_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${TEAMCITY_VERSION:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${TRAVIS:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${VELA:-}" ]]; then
      echo "true"
      return
    fi
    
    
    
    if [[ -n "${NOW_BUILDER:-}" ]] || [[ -n "${VERCEL:-}" ]]; then
      echo "true"
      return
    fi
    
    
    
    if [[ -n "${APPCENTER_BUILD_ID:-}" ]]; then
      echo "true"
      return
    fi
    
    
    
    
    if [[ "${CI:-}" == "woodpecker" ]]; then
      echo "true"
      return
      
    fi
    
    
    
    if [[ -n "${CI_XCODE_PROJECT:-}" ]]; then
      echo "true"
      return
    fi
    
    
    if [[ -n "${XCS:-}" ]]; then
      echo "true"
      return
    fi
    
    

    echo "false"
}

