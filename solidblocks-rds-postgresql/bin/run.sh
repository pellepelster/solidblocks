#!/usr/bin/env bash

set -eu -o pipefail

export DB_ADMIN_USERNAME="${USER}"
export DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:-$(uuidgen)}"
export POSTGRES_STOP_TIMEOUT="${POSTGRES_STOP_TIMEOUT:-60}"
export DB_MODE="${DB_MODE:-}"

FULL_BACKUP_NAME="full backup"
DIFF_BACKUP_NAME="differential backup"
INCR_BACKUP_NAME="incremental backup"
FULL_BACKUP_SCRIPT_NAME="backup-full.sh"
DIFF_BACKUP_SCRIPT_NAME="backup-diff.sh"
INCR_BACKUP_SCRIPT_NAME="backup-incr.sh"
CRON_DATABASE="postgres"

function log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S.%3N %Z")   [solidblocks-rds-postgresql] $*"
}

function current_major_version() {
  postgres --version | rg '([0-9]{1,})\.[0-9]{1,}' -or '$1'
}

function ensure_environment_variables() {
  for var in $@; do
    if [[ -z "${!var:-}" ]]; then
      log "'$var' is empty or not set"
      exit 1;
    fi
  done
}

########################################################
#              verify environment setup                #
########################################################
ensure_environment_variables DB_INSTANCE_NAME

if [[ $((DB_BACKUP_S3 + DB_BACKUP_LOCAL + DB_BACKUP_GCS)) == 0 ]]; then
  log "either 'DB_BACKUP_S3', 'DB_BACKUP_GCS' or 'DB_BACKUP_LOCAL' has to be activated"
  exit 1
fi

if [[ ${DB_BACKUP_S3:-0} == 1 ]]; then
  ensure_environment_variables DB_BACKUP_S3_BUCKET DB_BACKUP_S3_ACCESS_KEY DB_BACKUP_S3_SECRET_KEY

  if [[ -n "${DB_BACKUP_S3_CA_PUBLIC_KEY:-}" ]]; then
    mkdir -p /rds/certificates
    echo -n "${DB_BACKUP_S3_CA_PUBLIC_KEY}" | base64 -d > /rds/certificates/ca.pem
  fi
fi

if [[ ${DB_BACKUP_GCS:-0} == 1 ]]; then
  ensure_environment_variables DB_BACKUP_GCS_BUCKET DB_BACKUP_GCS_SERVICE_KEY_BASE64
  mkdir -p "/rds/gcs"
  echo "${DB_BACKUP_GCS_SERVICE_KEY_BASE64}" | base64 -d > "/rds/gcs/service-key.json"
  export DB_BACKUP_GCS_SERVICE_KEY_FILE="/rds/gcs/service-key.json"
fi

export DB_BACKUP_LOCAL_DIR="${DB_BACKUP_LOCAL_DIR:-/storage/backup}"

if [[ ${DB_BACKUP_LOCAL:-0} == 1 ]]; then
  ensure_environment_variables DB_BACKUP_LOCAL_DIR

  if ! mount | grep "${DB_BACKUP_LOCAL_DIR}"; then
      log "local backup dir '${DB_BACKUP_LOCAL_DIR}' not mounted"
      exit 1
  fi
fi

if ! mount | grep "${DATA_DIR}"; then
    log "storage dir '${DATA_DIR}' not mounted"
    exit 1
fi

rm -rf /rds/run/*

########################################################
#          persist certificates if provided            #
########################################################
if [[ -n "${SSL_SERVER_KEY:-}" ]] && [[ -n "${SSL_SERVER_CERT:-}" ]]; then
  mkdir -p /rds/ssl
  (
    cd "/rds/ssl"
    touch server.key
    chmod 600 server.key
    echo "${SSL_SERVER_KEY}" | base64 -d > server.key

    touch server.crt
    chmod 600 server.crt
    echo "${SSL_SERVER_CERT}" | base64 -d > server.crt
  )
fi

log "starting..."
log "postgres major version is $(current_major_version)"
log "s3 backup bucket: ${DB_BACKUP_S3_BUCKET:-<none>}"

export PG_DATA_BASE_DIR="${DATA_DIR}/${DB_INSTANCE_NAME}"

###########################################################
# migrate old data not in major version dependent folders #
###########################################################
if [[ -f "${PG_DATA_BASE_DIR}/PG_VERSION" ]]; then
  log "old directory layout detected, migrating data files from '${PG_DATA_BASE_DIR}' to '${PG_DATA_BASE_DIR}/$(current_major_version)'"

  if [[ ! -d "${PG_DATA_BASE_DIR}_$(current_major_version)" ]]; then
    mv "${PG_DATA_BASE_DIR}" "${PG_DATA_BASE_DIR}_$(current_major_version)"
  fi

  if [[ ! -d "${PG_DATA_BASE_DIR}/$(current_major_version)" ]]; then
    mkdir -p "${PG_DATA_BASE_DIR}"
    mv "${PG_DATA_BASE_DIR}_$(current_major_version)" "${PG_DATA_BASE_DIR}/$(current_major_version)"
  fi
fi

export PG_DATA_DIR="${PG_DATA_BASE_DIR}/$(current_major_version)"
export POSTGRES_BASE_DIR="/usr/libexec/postgresql$(current_major_version)"
export POSTGRES_BIN_DIR="${POSTGRES_BASE_DIR}"

log "setting PG_DATA_DIR to '${PG_DATA_DIR}'"
mkdir -p "${PG_DATA_DIR}"

gomplate --input-dir /rds/templates/config/ --output-map='/rds/config/{{ .in | strings.ReplaceAll ".template" "" }}'
gomplate --input-dir /rds/templates/bin/ --output-map='/rds/bin/{{ .in | strings.ReplaceAll ".template" "" }}'

function psql_execute() {
  local database=${1:-}
  local query=${2:-}
  psql -h /rds/socket --username "${DB_ADMIN_USERNAME}" --field-separator-zero --record-separator-zero --tuples-only --quiet --command "${query}" "${database}"
}

function pgbackrest_default_arguments() {
  echo "--config /rds/config/pgbackrest.conf --log-path=/rds/log --stanza=${DB_INSTANCE_NAME}"
}

function psql_execute_file() {
  local database=${1:-}
  local file=${2:-}
  psql -h /rds/socket --username "${DB_ADMIN_USERNAME}" --file "${file}" "${database}"
}

function psql_count() {
  psql_execute "${1}" "${2}" | tr -d '[:space:]'
}

function ensure_databases() {

    for database_var in "${!DB_DATABASE_@}"; do

      local database_id="${database_var#"DB_DATABASE_"}"
      local database="${!database_var:-}"
      if [[ -z "${database}" ]]; then
        echo "provided database name was empty"
        continue
      fi

      local database_user_var="DB_USERNAME_${database_id}"
      local username="${!database_user_var:-}"
      if [[ -z "${username}" ]]; then
        echo "no username provided for database '${database}'"
        continue
      fi


      local database_password_var="DB_PASSWORD_${database_id}"
      local password="${!database_password_var:-}"
      if [[ -z "${password}" ]]; then
        echo "no password provided for database '${database}'"
        continue
      fi

      log "ensuring database '${database}' with user '${username}'"
      ensure_database "${database}" "${database_id}"
      ensure_db_user "${database}" "${username}" "${password}"

    done

    # combining pg_cron with postgres <= 14 leads to migration issues when migration to later versions
    # since pg_cron is a newly introduced feature and postgres is already at version 17 cut off support for
    # pg_cron in older versions
    # TODO remove when solidblocks-docker image 14 is deprecated
    if [[ "$(current_major_version)" != 14 ]]; then
      ensure_backup
    fi
}

function db_option() {
  local database_id="${1:-}"
  local option_name="${2:-}"

  local var_name="DB_${option_name}_${database_id}"
  local option_value="${!var_name:-}"
  if [[ -z "${option_value}" ]]; then
    echo ""
  else
    echo "${option_name} = \"${option_value}\""
  fi
}

function ensure_database() {
  local database="${1:-}"
  local database_id="${2:-}"

  if [[ $(psql_count "postgres" "SELECT count(datname) FROM pg_database WHERE datname = '${database}';") == "0" ]]; then
    log "creating database '${database}'"

    local options="$(db_option "${database_id}" "ENCODING") $(db_option "${database_id}" "LC_COLLATE") $(db_option "${database_id}" "LC_CTYPE") $(db_option "${database_id}" "TEMPLATE")"
    if [[ -n "${options}" ]]; then
      options="WITH ${options}"
    fi

    echo "CREATE DATABASE \"${database}\" ${options}"
    psql_execute "postgres" "CREATE DATABASE \"${database}\" ${options}"

    local database_init_sql_var="DB_INIT_SQL_${database_id}"
    local database_init_sql="${!database_init_sql_var:-}"

    if [[ -n "${database_init_sql}" ]]; then
      if [[ -r "${database_init_sql}" ]]; then
        echo "executing init sql file '${database_init_sql}'"
        psql_execute_file "${database}" "${database_init_sql}"
      else
        echo "no init sql file found at '${database_init_sql}' or the file is not readable"
      fi
    fi

  fi
}

function ensure_permissions() {
  log "setting permissions for '${username}'"
  psql_execute "${database}" "GRANT ALL PRIVILEGES ON DATABASE \"${database}\" TO \"${username}\""
  psql_execute "${database}" "GRANT ALL PRIVILEGES ON SCHEMA public TO \"${username}\""
  psql_execute "${database}" "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \"${username}\""
  psql_execute "${database}" "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO \"${username}\""
}

function ensure_db_user() {
  local database="${1:-}"
  local username="${2:-}"
  local password="${3:-}"

  if [[ $(psql_count "${database}" "SELECT count(u.usename) FROM pg_catalog.pg_user u WHERE u.usename = '${username}';") == "0" ]]; then
    log "creating user '${username}'"
    psql_execute "${database}" "CREATE USER \"${username}\" WITH ENCRYPTED PASSWORD '${password}'"
  else
    log "setting password for '${username}'"
    psql_execute "${database}" "ALTER USER \"${username}\" WITH ENCRYPTED PASSWORD '${password}'"
  fi

  log "granting all privileges for '${username}' on '${database}'"

  if [[ -f "${PG_DATA_DIR}/solidblocks_current_db_username_${database}" ]]; then
    local last_db_username=$(cat "${PG_DATA_DIR}/solidblocks_current_db_username_${database}")

    if [[ "${last_db_username}" != "${username}" ]]; then
      log "reassigning ownerships from '${last_db_username}' to '${username}'"

      psql_execute "${database}" "REASSIGN OWNED BY \"${last_db_username}\" TO \"${username}\""
    fi
  fi

  ensure_permissions

  echo "${username}" > "${PG_DATA_DIR}/solidblocks_current_db_username_${database}"
}

function migrate_old_data_if_needed() {

  PREVIOUS_VERSION="$(($(current_major_version)-1))"
  PREVIOUS_PG_DATA_DIR="${PG_DATA_BASE_DIR}/${PREVIOUS_VERSION}"
  PREVIOUS_POSTGRES_BASE_DIR="/usr/libexec/postgresql${PREVIOUS_VERSION}"
  PREVIOUS_POSTGRES_BIN_DIR="${PREVIOUS_POSTGRES_BASE_DIR}"

  if [[ -f "${PREVIOUS_PG_DATA_DIR}/PG_VERSION" ]] && [[ -z "$(ls -A ${PG_DATA_DIR})" ]]; then

    init_db

    log "found old version data in '${PREVIOUS_PG_DATA_DIR}' and empty data dir '${PG_DATA_DIR}' migrating data"

    log "starting and stopping server in '${PREVIOUS_PG_DATA_DIR}' to ensure clean data dir"
    cp -v /rds/config/postgresql.conf "${PREVIOUS_PG_DATA_DIR}/postgresql.conf"
    cp -v /rds/config/pg_hba.conf "${PREVIOUS_PG_DATA_DIR}/pg_hba.conf"
    ${PREVIOUS_POSTGRES_BIN_DIR}/pg_ctl -D "${PREVIOUS_PG_DATA_DIR}" start --options="-c listen_addresses='' -c archive_mode=off -c shared_preload_libraries=''"
    ${PREVIOUS_POSTGRES_BIN_DIR}/pg_ctl -D "${PREVIOUS_PG_DATA_DIR}" stop --timeout ${POSTGRES_STOP_TIMEOUT}

    log "upgrading postgres data in '${PREVIOUS_PG_DATA_DIR}' to '${PG_DATA_DIR}'"
    pg_upgrade \
      --old-options "-c shared_preload_libraries=''" \
      --old-datadir=${PREVIOUS_PG_DATA_DIR} \
      --new-datadir=${PG_DATA_DIR} \
      --old-bindir=${PREVIOUS_POSTGRES_BIN_DIR} \
      --new-bindir=${POSTGRES_BIN_DIR}

    ${PREVIOUS_POSTGRES_BIN_DIR}/pg_ctl -D "${PREVIOUS_PG_DATA_DIR}" start --options="-c listen_addresses='' -c archive_mode=off -c shared_preload_libraries=''"
    pgbackrest $(pgbackrest_default_arguments) --db-path=${PG_DATA_DIR} --no-online stanza-upgrade
    ${PREVIOUS_POSTGRES_BIN_DIR}/pg_ctl -D "${PREVIOUS_PG_DATA_DIR}" stop --timeout ${POSTGRES_STOP_TIMEOUT}
  fi
}

function init_db() {
  log "initializing database instance"
  ${POSTGRES_BIN_DIR}/initdb --username="${DB_ADMIN_USERNAME}" --encoding=UTF8 --pwfile=<(echo "${DB_ADMIN_PASSWORD}") -D "${PG_DATA_DIR}" || true

  cp -v /rds/config/postgresql.conf "${PG_DATA_DIR}/postgresql.conf"
  cp -v /rds/config/pg_hba.conf "${PG_DATA_DIR}/pg_hba.conf"
}

function initialize_db_and_config() {
  init_db

  # make sure we only listen public when DB is ready to go
  ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" start --options="-c listen_addresses=''"

  pgbackrest $(pgbackrest_default_arguments) stanza-create
  ensure_databases

  log "executing initial backup"
  pgbackrest $(pgbackrest_default_arguments) --log-level-console=info --type=full backup

  log "stopping postgres"
  ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" stop --timeout ${POSTGRES_STOP_TIMEOUT}
}

function pgbackrest_status_code() {
  PGBACKREST_INFO=$(pgbackrest $(pgbackrest_default_arguments) --output=json info)

  if [[ $(echo ${PGBACKREST_INFO} | jq length) -gt 0 ]]; then
    BACKUP_INFO=$(echo ${PGBACKREST_INFO} | jq ".[] | select(.name == \"${DB_INSTANCE_NAME}\")")
    echo ${BACKUP_INFO} | jq -r '.status.code'
  else
    echo "99"
  fi
}

function pgbackrest_default_restore_arguments() {
  echo $(pgbackrest_default_arguments) --db-path=${PG_DATA_DIR} restore --recovery-option="recovery_end_command=/rds/bin/recovery_complete.sh"
}

function start_db_restore_only() {
  export DB_MODE="restore-only"
  log "starting db in restore-only mode"
  start_db
}

function create_db_extension() {
  local DB_EXTENSION_NAME=$1
  psql_execute ${CRON_DATABASE} "CREATE EXTENSION IF NOT EXISTS ${DB_EXTENSION_NAME}"
}

function unschedule_backup() {
  local BACKUP_NAME=$1
  psql_execute ${CRON_DATABASE} "select cron.unschedule('${BACKUP_NAME}')" > /dev/null || true
}

function schedule_backup() {
  local BACKUP_NAME=$1
  local BACKUP_SCHEDULE=$2
  local BACKUP_SCRIPT_NAME=$3
  if [ -n "${BACKUP_SCHEDULE}" ]
  then
  	psql_execute ${CRON_DATABASE} "select cron.schedule('${BACKUP_NAME}','${BACKUP_SCHEDULE}', 'select pg_remote_exec_fetch(''/rds/bin/${BACKUP_SCRIPT_NAME}'',''t'')')" > /dev/null
  	log "scheduled ${BACKUP_NAME} with CRON expression: ${BACKUP_SCHEDULE}"
  fi
}

function ensure_backup() {
  log "ensuring backup is scheduled"
  create_db_extension "pg_cron"
  create_db_extension "pg_remote_exec"
  unschedule_backup "${FULL_BACKUP_NAME}"
  unschedule_backup "${DIFF_BACKUP_NAME}"
  unschedule_backup "${INCR_BACKUP_NAME}"
  schedule_backup "${FULL_BACKUP_NAME}" "${DB_BACKUP_FULL_SCHEDULE}" ${FULL_BACKUP_SCRIPT_NAME}
  schedule_backup "${DIFF_BACKUP_NAME}" "${DB_BACKUP_DIFF_SCHEDULE}" ${DIFF_BACKUP_SCRIPT_NAME}
  schedule_backup "${INCR_BACKUP_NAME}" "${DB_BACKUP_INCR_SCHEDULE}" ${INCR_BACKUP_SCRIPT_NAME}
}

function start_db() {
  migrate_old_data_if_needed

  if [[ -z "$(ls -A ${PG_DATA_DIR})" ]]; then
    log "data dir is empty"


    if [[ $(pgbackrest_status_code) -eq 0 ]]; then

      #log "restoring database from backup (latest)"
      #pgbackrest --config /rds/config/pgbackrest.conf --log-path=/rds/log  --log-level-console=info --stanza=${DB_INSTANCE_NAME} stanza-upgrade

      if [[ -z "${DB_RESTORE_PITR:-}" ]]; then
        log "restoring database from backup (latest)"
        pgbackrest $(pgbackrest_default_restore_arguments)
      else
        log "restoring database from backup (${DB_RESTORE_PITR})"
        pgbackrest $(pgbackrest_default_restore_arguments) --type=time --target="${DB_RESTORE_PITR}" --target-action=promote
      fi

      sleep 5

      log "starting db for recovery"
      ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" start --options="-c listen_addresses=''"

      while [[ -f /tmp/recovery_complete ]]; do
        log "waiting for recovery completion"
        sleep 5
      done

      until [[ "$(psql_execute "postgres" 'SELECT pg_is_in_recovery();' | tr -d '[:space:]')" == "f" ]]; do
        log "waiting for server to be ready"
        sleep 5
      done

      ensure_databases

      log "stopping postgres"
      ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" stop --timeout ${POSTGRES_STOP_TIMEOUT}
    else
      initialize_db_and_config
    fi
  else
    log "data dir is not empty"

    rm -f /rds/socket/*
    rm -f "${PG_DATA_DIR}/postmaster.pid"

    cp -v /rds/config/postgresql.conf "${PG_DATA_DIR}/postgresql.conf"
    cp -v /rds/config/pg_hba.conf "${PG_DATA_DIR}/pg_hba.conf"

    ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" start --options="-c listen_addresses=''"

    pgbackrest $(pgbackrest_default_arguments) stanza-create
    ensure_databases

    log "setting password for '${DB_ADMIN_USERNAME}'"
    psql_execute "postgres" "ALTER USER \"${DB_ADMIN_USERNAME}\" WITH ENCRYPTED PASSWORD '${DB_ADMIN_PASSWORD}'"

    ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" stop --timeout ${POSTGRES_STOP_TIMEOUT}
  fi

  cp -v /rds/config/postgresql.conf "${PG_DATA_DIR}/postgresql.conf"
  cp -v /rds/config/pg_hba.conf "${PG_DATA_DIR}/pg_hba.conf"

  log "provisioning completed"
  touch /rds/run/provisioning_completed
  exec ${POSTGRES_BIN_DIR}/postgres -D "${PG_DATA_DIR}"
}

function maintenance() {
  while true;  do
    log "maintenance mode is active, no database is started"
    sleep 5
  done
}

ARG=${1:-}
shift || true

case "${ARG}" in
  maintenance) maintenance;;
  restore-only) start_db_restore_only;;
  *) start_db ;;
esac