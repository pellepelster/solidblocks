function backup_url() {
  echo "ssh://${CLOUD_NAME}@backup.${CLOUD_NAME}.${ROOT_DOMAIN}/~/borg"
}

function backup_borg_wrapper() {
    CONSUL_HTTP_TOKEN=$(config '.consul_master_token') BORG_PASSPHRASE="$(config .backup_password)" consul lock -child-exit-code "solidblocks/instance/${SOLIDBLOCKS_INSTANCE_ID}/backup/borg-lock" borg "$@"
}

function backup_init() {
    local url=${1:-}
    backup_borg_wrapper init --encryption=repokey "${url}"
}

function backup_info() {
    local full_url=${1:-}
    backup_borg_wrapper info "${full_url}"
}

function backup_init_if_needed() {

    if [[ "$(consul_kv_get "solidblocks/instance/${SOLIDBLOCKS_INSTANCE_ID}/backup/initialized")" == "true" ]]; then
        echo "backup already initialized"
    else
        consul_kv_put "solidblocks/instance/${SOLIDBLOCKS_INSTANCE_ID}/backup/initialized" "true"

        while [[ ! $(backup_info "$(backup_url)") ]]; do
            backup_init "$(backup_url)" || true
            echo "waiting for valid repository at '$(backup_url)'..."
            sleep 10
        done
    fi
}

function backup_step_wait() {
    for host in "$@"
    do
        wait_for_consul_key "solidblocks/backup/jobs/${NOMAD_META_BACKUP_ID}/${host}-prepare-finished" 10 30
    done
}

function backup_script_full() {
    local name=${1:-}
    local dir=${2:-}
cat <<-EOF
#!/usr/bin/env bash
set -eux

source /solidblocks/instance/environment
source /solidblocks/lib/configuration.sh
source /solidblocks/lib/config.sh
source /solidblocks/lib/backup.sh

backup_full "${name}" "${dir}"
EOF
}

function backup_nomad_tasks_script() {
cat <<-EOF
#!/usr/bin/env bash
set -eux

source /solidblocks/instance/environment
source /solidblocks/lib/configuration.sh
source /solidblocks/lib/config.sh
source /solidblocks/lib/backup.sh

for backup_script in \${NOMAD_TASK_DIR}/nomad/alloc/*/*/bin/backup.sh; do
	bash \${backup_script}
done
EOF
}

function backup_script_storage() {
    local storage_local_name=${1:-}
    local storage_local_dir=${2:-}
    shift || true
    shift || true
cat <<-EOF
#!/usr/bin/env bash
set -eux

source /solidblocks/instance/environment
source /solidblocks/lib/configuration.sh
source /solidblocks/lib/config.sh
source /solidblocks/lib/backup.sh

backup_step_wait \$@

backup_full "storage_${storage_local_name}" "${storage_local_dir}"
EOF
}

function backup_prepare_script_storage() {
cat <<-EOF
#!/usr/bin/env bash
set -eux
EOF
}

function backup_script_worker() {
cat <<-EOF
#!/usr/bin/env bash
set -eux
EOF
}

function node_backup_script() {
cat <<-EOF
#!/usr/bin/env bash
set -eux

run-parts --exit-on-error --verbose /solidblocks/backup-prepare.d/
run-parts --exit-on-error --verbose /solidblocks/backup.d/
EOF
}

function backup_prepare_script_docker() {
    local storage_local_name=${1:-}
    local storage_local_dir=${2:-}
cat <<-EOF
#!/usr/bin/env bash

set -eux

CONTAINERS=\$(docker ps --no-trunc --filter "label=SOLIDBLOCKS_BACKUP_SCRIPT" --format "{{.ID}}")

for container in \${CONTAINERS}; do
	name=\$(docker inspect --format '{{.Name}}' "\${container}")
	backup_script=\$(docker inspect --format '{{index .Config.Labels "SOLIDBLOCKS_BACKUP_SCRIPT"}}' "\${container}")
	echo "executing script '\${backup_script}' for '\${name}'"
	docker exec "\${container}" "\${backup_script}"
done
EOF
}

function backup_full() {
    local archive_name=${1:-}
    local backup_dir=${2:-}
    shift || true
    shift || true
    (
        cd "${backup_dir}" || exit
        backup_borg_wrapper create --progress "$(backup_url)::node_{hostname}_${archive_name}_{now:%Y-%m-%dT%H:%M:%S}" "./" "$@"
    )
}

function backup_restore() {

	local pattern="${1:-}"
	local restore_dir="${2:-}"
	local list_result

	list_result=$(backup_borg_wrapper list --glob-archives "${pattern}" --json --sort-by timestamp "$(backup_url)")

	if [[ $(echo "${list_result}"| jq '.archives | length') -eq 0 ]]; then
		echo "no backups found to restore"
	else
		local archive
		archive=$(echo "${list_result}" | jq -r '.archives[-1].archive')
    (
      cd "${restore_dir}" || exit
      backup_borg_wrapper extract "${BORG_RESTORE_EXTRA_OPTIONS:-}" --list "$(backup_url)::${archive}"
		)
	fi
}