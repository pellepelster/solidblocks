#!/usr/bin/env bash

export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"
export HETZNER_DNS_API_TOKEN="${HETZNER_DNS_API_TOKEN:-$(pass solidblocks/hetzner/test/dns_api_token)}"
export TF_VAR_hcloud_token="${HCLOUD_TOKEN}"

function task_test_terraform {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  (
    cd "test/terraform"
    terraform init -upgrade
    terraform apply -auto-approve
  )
}

function task_test_ansible {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  python_ensure_venv "${DIR}"

  "${VENV_DIR}/bin/ansible-galaxy" \
    collection install \
    --force \
    -v \
    -r "test/ansible/requirements.yml"

  "${VENV_DIR}/bin/ansible-playbook" \
    -i "test/terraform/output/test/cluster1/ansible/blcks_k3s_inventory.yml" \
    "--extra-vars" "@test/terraform/output/test/cluster1/ansible/blcks_k3s_variables.yml" \
    "--extra-vars" "@test/terraform/output/test/cluster1/ansible/blcks_k3s_hetzner_variables.yml" \
    "test/ansible/site.yml"
}

function task_test_dual_terraform {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  (
    cd "test/terraform-dual"
    terraform init -upgrade
    terraform apply -auto-approve
  )
}

function task_test_dual_ansible {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  python_ensure_venv "${DIR}"

  "${VENV_DIR}/bin/ansible-galaxy" \
    collection install \
    --force \
    -v \
    -r "test/ansible/requirements.yml"

  "${VENV_DIR}/bin/ansible-playbook" \
    -i "test/terraform-dual/output/test/cluster1-blue/ansible/blcks_k3s_inventory.yml" \
    "--extra-vars" "@test/terraform-dual/output/test/cluster1-blue/ansible/blcks_k3s_variables.yml" \
    "--extra-vars" "@test/terraform-dual/output/test/cluster1-blue/ansible/blcks_k3s_hetzner_variables.yml" \
    "test/ansible/site.yml"

  "${VENV_DIR}/bin/ansible-playbook" \
    -i "test/terraform-dual/output/test/cluster1-green/ansible/blcks_k3s_inventory.yml" \
    "--extra-vars" "@test/terraform-dual/output/test/cluster1-green/ansible/blcks_k3s_variables.yml" \
    "--extra-vars" "@test/terraform-dual/output/test/cluster1-green/ansible/blcks_k3s_hetzner_variables.yml" \
    "test/ansible/site.yml"
}

function test_helm {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  local ssh_config="${1:-}"
  local server="${2:-}"
  local api_address="${3:-}"
  local hello_world_domain="${4:-}"

  mkdir -p "output/"
  scp -F "${ssh_config}" ${server}:/etc/rancher/k3s/k3s.yaml "output/"
  sed -i "s/https:\/\/127\.0\.0\.1:6443/https:\/\/${api_address}:6443/" "output/k3s.yaml"

  helm --kubeconfig output/k3s.yaml \
    --namespace hello-world \
    upgrade \
    --install \
    --create-namespace \
    --set domain.name="${hello_world_domain}" \
    --values test/helm/hello-world/values.yml \
    hello-world test/helm/blcks-service/
}

function task_test_dual_helm {

  test_helm \
    "test/terraform-dual/output/test/cluster1-blue/ssh/client_config" \
    "test-cluster1-blue-k3s-server-0" \
    "k3s-api.cluster1-blue.blcks.de" \
    "hello-world.cluster1-blue"

  test_helm \
    "test/terraform-dual/output/test/cluster1-green/ssh/client_config" \
    "test-cluster1-green-k3s-server-0" \
    "k3s-api.cluster1-green.blcks.de" \
    "hello-world.cluster1-green"
}

function task_test_helm {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  mkdir -p "output/"
  scp -F "test/terraform/output/test/cluster1/ssh/client_config" test-cluster1-k3s-server-0:/etc/rancher/k3s/k3s.yaml "output/"
  sed -i 's/https:\/\/127\.0\.0\.1:6443/https:\/\/test-k3s.blcks.de:6443/' "output/k3s.yaml"

# --set domain.name=$(tr -dc a-z </dev/urandom | head -c 13; echo) \
helm --kubeconfig output/k3s.yaml \
    --namespace hello-world \
    upgrade \
    --install \
    --create-namespace \
    --values test/helm/hello-world/values.yml \
    hello-world test/helm/blcks-service/
}

function task_test_helm {
  test_helm \
    "test/terraform/output/test/cluster1/ssh/client_config" \
    "test-cluster1-k3s-server-0" \
    "k3s-api.cluster1.blcks.de" \
    "hello-world.cluster1"
}
