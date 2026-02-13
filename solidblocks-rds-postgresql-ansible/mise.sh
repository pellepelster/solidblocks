#!/usr/bin/env bash

function prepare_env() {
  export HCLOUD_TOKEN="${HCLOUD_TOKEN:-$(pass solidblocks/hetzner/test/hcloud_api_token)}"
  export SCW_ACCESS_KEY="${SCW_ACCESS_KEY:-$(pass solidblocks/scaleway/test/access_key_id)}"
  export SCW_SECRET_KEY="${SCW_SECRET_KEY:-$(pass solidblocks/scaleway/test/secret_key)}"
  export SCW_DEFAULT_ORGANIZATION_ID="${SCW_DEFAULT_ORGANIZATION_ID:-$(pass solidblocks/scaleway/oranization_id)}"
  export TF_VAR_scw_organization_id="${SCW_DEFAULT_ORGANIZATION_ID}"
  export TF_VAR_environment="test"
  export TF_VAR_location="nbg1"
  export TF_VAR_name="$(cat "test.id")"
  export AWS_DEFAULT_REGION="eu-central-1"
  export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/aws/admin/access_key_id)}" #
  export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/aws/admin/secret_access_key)}"
}

function ensure_test_id() {
  if [[ ! -f "test.id" ]]; then
    tr -dc a-z </dev/urandom | head -c 8 > "test.id"
  fi
  
  export TEST_ID="$(cat "test.id")"
}

function task_test_standalone() {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi
  
  ensure_test_id

  echo "====================================================================================="
  echo "task_test_standalone_terraform"
  echo "====================================================================================="
  task_test_standalone_terraform

  echo "====================================================================================="
  echo "task_test_standalone_ansible"
  echo "====================================================================================="
  task_test_standalone_ansible

  echo "====================================================================================="
  echo "task_test_standalone_after_bootstrap"
  echo "====================================================================================="
  task_test_standalone_after_bootstrap

  echo "====================================================================================="
  echo "task_full_backup_standalone"
  echo "====================================================================================="
  task_full_backup_standalone

  echo "====================================================================================="
  echo "task_test_standalone_destroy_instances"
  echo "====================================================================================="
  task_test_standalone_destroy_instances

  echo "====================================================================================="
  echo "task_test_standalone_terraform"
  echo "====================================================================================="
  task_test_standalone_terraform

  echo "====================================================================================="
  echo "task_test_standalone_ansible"
  echo "====================================================================================="
  task_test_standalone_ansible

  echo "====================================================================================="
  echo "task_test_standalone_after_instance_rebuild"
  echo "====================================================================================="
  task_test_standalone_after_instance_rebuild

  echo "====================================================================================="
  echo "task_test_standalone_destroy_instances"
  echo "====================================================================================="
  task_test_standalone_destroy_instances

  echo "====================================================================================="
  echo "task_test_standalone_destroy_storage"
  echo "====================================================================================="
  task_test_standalone_destroy_storage

  echo "====================================================================================="
  echo "task_test_standalone_terraform"
  echo "====================================================================================="
  task_test_standalone_terraform

  echo "====================================================================================="
  echo "task_test_standalone_ansible"
  echo "====================================================================================="
  task_test_standalone_ansible

  echo "====================================================================================="
  echo "task_test_standalone_after_storage_rebuild"
  echo "====================================================================================="
  task_test_standalone_after_storage_rebuild

  echo "====================================================================================="
  echo "task_test_standalone_ansible 17"
  echo "====================================================================================="
  task_test_standalone_ansible 17

  echo "====================================================================================="
  echo "task_test_standalone_after_upgrade"
  echo "====================================================================================="
  task_test_standalone_after_upgrade

  echo "====================================================================================="
  echo "task_test_standalone_ansible 17"
  echo "====================================================================================="
  task_test_standalone_ansible 17

  echo "====================================================================================="
  echo "task_test_standalone_after_upgrade"
  echo "====================================================================================="
  task_test_standalone_after_upgrade

  echo "====================================================================================="
  echo "task_full_backup_standalone"
  echo "====================================================================================="
  task_full_backup_standalone

  echo "====================================================================================="
  echo "task_test_standalone_destroy_instances"
  echo "====================================================================================="
  task_test_standalone_destroy_instances

  echo "====================================================================================="
  echo "task_test_standalone_destroy_storage"
  echo "====================================================================================="
  task_test_standalone_destroy_storage

  echo "====================================================================================="
  echo "task_test_standalone_terraform"
  echo "====================================================================================="
  task_test_standalone_terraform

  echo "====================================================================================="
  echo "task_test_standalone_ansible"
  echo "====================================================================================="
  task_test_standalone_ansible 17
}

function task_test_standalone_terraform {
  prepare_env

  test_terraform "storage-standalone" init -upgrade
  test_terraform "storage-standalone" apply -auto-approve

  test_terraform "backup" init -upgrade
  test_terraform "backup" apply -auto-approve

  export TF_VAR_backup_s3_bucket=$(test_terraform "backup" output backup_s3_bucket)
  export TF_VAR_backup_s3_endpoint=$(test_terraform "backup" output backup_s3_endpoint)
  export TF_VAR_backup_s3_key=$(test_terraform "backup" output backup_s3_key)
  export TF_VAR_backup_s3_key_secret=$(test_terraform "backup" output backup_s3_key_secret)
  export TF_VAR_backup_s3_region=$(test_terraform "backup" output backup_s3_region)

  test_terraform "standalone" init -upgrade
  test_terraform "standalone" apply -auto-approve

  local database1_blue_ip=$(test_terraform "standalone" output -raw database1_blue_ip)
  network_wait_for_port_open ${database1_blue_ip} 22
}

function task_test_cluster_terraform {
  prepare_env

  test_terraform "storage-cluster" init -upgrade
  test_terraform "storage-cluster" apply -auto-approve

  test_terraform "backup" init -upgrade
  test_terraform "backup" apply -auto-approve

  export TF_VAR_backup_s3_bucket=$(test_terraform "backup" output backup_s3_bucket)
  export TF_VAR_backup_s3_endpoint=$(test_terraform "backup" output backup_s3_endpoint)
  export TF_VAR_backup_s3_key=$(test_terraform "backup" output backup_s3_key)
  export TF_VAR_backup_s3_key_secret=$(test_terraform "backup" output backup_s3_key_secret)
  export TF_VAR_backup_s3_region=$(test_terraform "backup" output backup_s3_region)

  test_terraform "cluster" init -upgrade
  test_terraform "cluster" apply -auto-approve

  local database2_green_ip=$(test_terraform "cluster" output -raw database2_green_ip)
  network_wait_for_port_open ${database2_green_ip} 22

  local database1_blue_ip=$(test_terraform "cluster" output -raw database1_blue_ip)
  network_wait_for_port_open ${database1_blue_ip} 22
}

function task_test_standalone_destroy_instances {
  prepare_env

  export TF_VAR_backup_s3_bucket=$(test_terraform "backup" output backup_s3_bucket)
  export TF_VAR_backup_s3_endpoint=$(test_terraform "backup" output backup_s3_endpoint)
  export TF_VAR_backup_s3_key=$(test_terraform "backup" output backup_s3_key)
  export TF_VAR_backup_s3_key_secret=$(test_terraform "backup" output backup_s3_key_secret)
  export TF_VAR_backup_s3_region=$(test_terraform "backup" output backup_s3_region)

  test_terraform "standalone" destroy -auto-approve
}

function task_test_standalone_destroy_storage {
  prepare_env
  test_terraform "storage-standalone" destroy -auto-approve
}

function ssh_wrapper() {
  ssh -F "test/terraform/output/test/standalone/ssh/client_config" test-${TEST_ID}-database1-blue "$@"
}

function task_full_backup_standalone {
  ssh_wrapper /usr/local/bin/test-database1-backup-full.sh
}

function task_test_standalone_after_bootstrap {
  python_ensure_venv "${DIR}"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/standalone/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database1-blue" "test/test-database1-blue-after-bootstrap.py"
}

function task_test_cluster_failover {
  ssh -F "test/terraform/output/test/cluster/ssh/client_config" test-${TEST_ID}-database2-blue 'systemctl stop test-database2'
  ssh -F "test/terraform/output/test/cluster/ssh/client_config" test-${TEST_ID}-database2-green 'rm /storage/data/test/database2/postgresql.auto.conf'
  ssh -F "test/terraform/output/test/cluster/ssh/client_config" test-${TEST_ID}-database2-green 'systemctl restart test-database2'
  ssh -F "test/terraform/output/test/cluster/ssh/client_config" test-${TEST_ID}-database2-green 'test-database2-psql postgres -c "SELECT pg_promote();"'
  ssh -F "test/terraform/output/test/cluster/ssh/client_config" test-${TEST_ID}-database2-green 'test-database2-backup-incr.sh'
  ssh -F "test/terraform/output/test/cluster/ssh/client_config" test-${TEST_ID}-database2-blue 'rm -rf /storage/data/test/'
}

function task_test_cluster_after_bootstrap {
  python_ensure_venv "${DIR}"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/cluster/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database2-blue" "test/test-database2-blue-after-bootstrap.py"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/cluster/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database2-green" "test/test-database2-green-after-bootstrap.py"
}

function task_test_cluster_after_failover {
  python_ensure_venv "${DIR}"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/cluster/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database2-blue" "test/test-database2-blue-after-failover.py"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/cluster/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database2-green" "test/test-database2-green-after-failover.py"
}

function task_test_standalone_after_instance_rebuild {
  python_ensure_venv "${DIR}"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/standalone/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database1-blue" "test/test-database1-blue-after-instance-rebuild.py"
}

function task_test_standalone_after_storage_rebuild {
  python_ensure_venv "${DIR}"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/standalone/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database1-blue" "test/test-database1-blue-after-storage-rebuild.py"
}

function task_test_standalone_after_upgrade {
  python_ensure_venv "${DIR}"

  ".venv/bin/py.test" \
    --capture=tee-sys \
    --ssh-config="test/terraform/output/test/standalone/ssh/client_config" \
    --hosts="ssh://test-${TEST_ID}-database1-blue" "test/test-database1-blue-after-upgrade.py"
}

function task_test_standalone_ansible {
  prepare_env
  python_ensure_venv "${DIR}"
  local postgres_version=${1:-16}
  shift || true
  local database1_primary_node=$(test_terraform "standalone" output database1_blue_name)

  ".venv/bin/ansible-galaxy" \
    collection install \
    --force \
    -v \
    -r "test/ansible/standalone/requirements.yml"

  ".venv/bin/ansible-playbook" \
    -i "test/terraform/output/test/standalone/ansible/blcks_rds_postgres_inventory.yml" \
    --extra-vars "@test/terraform/output/test/standalone/ansible/blcks_rds_postgres_variables.yml" \
    --extra-vars "database1_primary_node=${database1_primary_node}" \
    --extra-vars "postgres_version=${postgres_version}" \
    "test/ansible/standalone/site.yml" "$@"
}

function task_test_cluster_ansible {
  prepare_env
  python_ensure_venv "${DIR}"
  local database2_primary_node=$(test_terraform "cluster" output database2_blue_name)

  ".venv/bin/ansible-galaxy" \
    collection install \
    --force \
    -v \
    -r "test/ansible/cluster/requirements.yml"

  ".venv/bin/ansible-playbook" \
    -i "test/terraform/output/test/cluster/ansible/blcks_rds_postgres_inventory.yml" \
    --extra-vars "@test/terraform/output/test/cluster/ansible/blcks_rds_postgres_variables.yml" \
    --extra-vars "database2_primary_node=${database2_primary_node}" \
    "test/ansible/cluster/site.yml" "$@"
}

function task_test_cluster_ansible_after_failover {
  prepare_env
  python_ensure_venv "${DIR}"
  local database2_primary_node=$(test_terraform "cluster" output database2_green_name)

  ".venv/bin/ansible-galaxy" \
    collection install \
    --force \
    -v \
    -r "test/ansible/cluster/requirements.yml"

  ".venv/bin/ansible-playbook" \
    -i "test/terraform/output/test/cluster/ansible/blcks_rds_postgres_inventory.yml" \
    --extra-vars "@test/terraform/output/test/cluster/ansible/blcks_rds_postgres_variables.yml" \
    --extra-vars "database2_primary_node=${database2_primary_node}" \
    "test/ansible/cluster/site.yml" "$@"
}

function task_test_cluster() {
  if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
    echo "skipping integration tests"
    exit 0
  fi

  ensure_test_id

  echo "====================================================================================="
  echo "task_test_cluster_terraform"
  echo "====================================================================================="
  task_test_cluster_terraform

  echo "====================================================================================="
  echo "task_test_cluster_ansible"
  echo "====================================================================================="
  task_test_cluster_ansible

  echo "====================================================================================="
  echo "task_test_cluster_after_bootstrap"
  echo "====================================================================================="
  task_test_cluster_after_bootstrap

  echo "====================================================================================="
  echo "task_test_cluster_failover"
  echo "====================================================================================="
  task_test_cluster_failover

  echo "====================================================================================="
  echo "task_test_cluster_ansible_after_failover"
  echo "====================================================================================="
  task_test_cluster_ansible_after_failover

  echo "====================================================================================="
  echo "task_test_cluster_after_failover"
  echo "====================================================================================="
  task_test_cluster_after_failover
}

function s3cmd_wrapper() {
    s3cmd \
      --access_key ${SCW_ACCESS_KEY_ID} \
      --secret_key ${SCW_SECRET_ACCESS_KEY} \
      --host "s3.${SCW_REGION}.scw.cloud" \
      --bucket-location "${SCW_REGION}" \
      --ssl \
      $@
}

function task_clean_data {
    export SCW_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-$(pass solidblocks/scaleway/test/access_key_id)}" #
    export SCW_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-$(pass solidblocks/scaleway/test/secret_key)}"
    export SCW_REGION="fr-par"

    if [[ "${SKIP_TESTS:-}" =~ .*integration.* ]]; then
      echo "skipping integration tests"
      exit 0
    fi

    ensure_test_id

    bucket="test-${TEST_ID}-backup"
    echo "removing objects from bucket '${bucket}'"

    local buckets=$(s3cmd_wrapper ls | awk '{print $NF}')

    for bucket in ${buckets}; do
      local bucket_name=${bucket#"s3://"}
      echo "cleaning objects in bucket '${bucket}'"
      s3cmd_wrapper rm ${bucket} \
        --recursive \
        --host-bucket "${bucket_name}.s3.${SCW_REGION}.scw.cloud" \
        --force
    done
}

function task_clean {

    task_clean_data

    test_terraform "storage-standalone" destroy -auto-approve || true
    test_terraform "backup" destroy -auto-approve || true
    test_terraform "standalone" destroy -auto-approve || true

    rm -f "test.id" || true
    rm -rf "build" || true
}

function test_terraform {
  prepare_env
  (
    local module="${1:-}"
    shift || true
    cd "test/terraform/${module}"
    terraform  $@
  )
}
