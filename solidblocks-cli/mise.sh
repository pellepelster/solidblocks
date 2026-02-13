#!/usr/bin/env bash
set -eu -o pipefail

function integration_test_blcks_wrapper() {
  if [[ -d "blcks" ]]; then
    "./blcks/bin/blcks" $@
  else
    "./blcks" $@
  fi
}

function divider_header() {
  echo "==================================================================================="
  echo $@
  echo "-----------------------------------------------------------------------------------"
}

function divider_footer() {
  echo "==================================================================================="
  echo ""
  echo ""
}

function task_test_hetzner_nuke {
  export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"

  divider_header "--help"
  integration_test_blcks_wrapper --help
  divider_footer

  divider_header "hetzner --help"
  integration_test_blcks_wrapper hetzner --help
  divider_footer

  divider_header "hetzner nuke --help"
  integration_test_blcks_wrapper hetzner nuke --help
  divider_footer

  divider_header "hetzner nuke"
  integration_test_blcks_wrapper hetzner nuke
  divider_footer

  divider_header "hetzner nuke --do-nuke"
  integration_test_blcks_wrapper hetzner nuke --do-nuke
  divider_footer
}

function task_test_hetzner_asg {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    exit 0
  fi

  task_init_testbed

  divider_header " hetzner asg --help"
  integration_test_blcks_wrapper hetzner asg --help
  divider_footer

  divider_header " hetzner asg rotate --help"
  integration_test_blcks_wrapper hetzner asg rotate --help
  divider_footer

  integration_test_blcks_wrapper hetzner asg rotate --loadbalancer application1 --user-data ${DIR}/test/terraform/cloud_init.template --replicas 1
}

function task_test_terraform {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    exit 0
  fi

  export AWS_REGION="eu-central-1"
  export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/admin/access_key_id)}"
  export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/admin/secret_access_key)}"

  divider_header "--help"
  integration_test_blcks_wrapper --help
  divider_footer

  divider_header "terraform --help"
  integration_test_blcks_wrapper terraform --help
  divider_footer

  divider_header "tofu --help"
  integration_test_blcks_wrapper tofu --help
  divider_footer

  divider_header "terraform backends --help"
  integration_test_blcks_wrapper terraform backends --help
  divider_footer

  divider_header "tofu backends --help"
  integration_test_blcks_wrapper tofu backends --help
  divider_footer

  divider_header "terraform backends s3 --help"
  integration_test_blcks_wrapper terraform backends s3 --help
  divider_footer

  divider_header "tofu backends s3 --help"
  integration_test_blcks_wrapper tofu backends s3 --help
  divider_footer

  local bucket="test-terraform-$(uuidgen)"

  divider_header "terraform backends s3"
  integration_test_blcks_wrapper terraform backends s3 "${bucket}"
  integration_test_blcks_wrapper terraform backends s3 "${bucket}"
  divider_footer

  divider_header "terraform backends --file /tmp/${bucket}.tf s3"
  integration_test_blcks_wrapper terraform backends s3 --file "/tmp/${bucket}.tf" "${bucket}"
  divider_footer

  bucket="test-tofu-$(uuidgen)"

  divider_header "tofu backends s3"
  integration_test_blcks_wrapper tofu backends s3 "${bucket}"
  divider_footer

  divider_header "tofu backends --file /tmp/${bucket}.tf s3"
  integration_test_blcks_wrapper tofu backends s3 --file "/tmp/${bucket}.tf" "${bucket}"
  divider_footer
}
