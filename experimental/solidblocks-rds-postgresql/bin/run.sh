#!/usr/bin/env bash

set -eu

function log() {
  echo "[solidblocks-rds-postgresql] $*"
}

function ensure_environment_variables() {
  for var in $@; do
    if [[ -z "${!var:-}" ]]; then
      log "'$var' is empty or not set"
      exit 1;
    fi
  done
}

ensure_environment_variables DB_DATABASE DB_PASSWORD DB_USERNAME

if [[ $((DB_BACKUP_S3 + DB_BACKUP_LOCAL)) == 0 ]]; then
  log "either 'DB_BACKUP_S3' or 'DB_BACKUP_LOCAL' has to be activated"
  exit 1
fi

if [[ ${DB_BACKUP_S3:-0} == 1 ]]; then
  ensure_environment_variables DB_BACKUP_S3_CA_PUBLIC_KEY DB_BACKUP_S3_HOST DB_BACKUP_S3_BUCKET DB_BACKUP_S3_ACCESS_KEY DB_BACKUP_S3_SECRET_KEY

  if [[ -n "${DB_BACKUP_S3_CA_PUBLIC_KEY:-}" ]]; then
    mkdir -p /rds/certificates
    echo -n "${DB_BACKUP_S3_CA_PUBLIC_KEY}" | base64 -d > /rds/certificates/ca.pem
  fi
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

log "starting..."

export PG_DATA_DIR="${DATA_DIR}/${DB_DATABASE}"

gomplate --input-dir /rds/templates/config/ --output-map='/rds/config/{{ .in | strings.ReplaceAll ".template" "" }}'
gomplate --input-dir /rds/templates/bin/ --output-map='/rds/bin/{{ .in | strings.ReplaceAll ".template" "" }}'

POSTGRES_BASE_DIR="/usr/libexec/postgresql14"
POSTGRES_BIN_DIR="${POSTGRES_BASE_DIR}"

mkdir -p "${PG_DATA_DIR}"

function psql_execute() {
  local database=${1:-}
  local query=${2:-}
  psql -h /rds/socket --username "${DB_ADMIN_USERNAME}" --field-separator-zero --record-separator-zero --tuples-only --quiet -c "${query}" "${database}"
}

function pgbackrest_execute() {
  pgbackrest --config /rds/config/pgbackrest.conf --log-path=/rds/log --stanza=${DB_DATABASE} "$@"
}

function psql_count() {
  psql_execute "${1}" "${2}" | tr -d '[:space:]'
}

function ensure_database() {
  if [[ $(psql_count "postgres" "SELECT count(datname) FROM pg_database WHERE datname = '${DB_DATABASE}';") == "0" ]]; then
    log "creating database '${DB_DATABASE}'"
    psql_execute "postgres" "CREATE DATABASE \"${DB_DATABASE}\""
  fi
}

function ensure_user() {
  if [[ $(psql_count "${DB_DATABASE}" "SELECT count(u.usename) FROM pg_catalog.pg_user u WHERE u.usename = '${DB_USERNAME}';") == "0" ]]; then
    log "creating user '${DB_USERNAME}'"
    psql_execute "${DB_DATABASE}" "CREATE USER \"${DB_USERNAME}\" WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"
  else
    log "setting password for '${DB_USERNAME}'"
    psql_execute "${DB_DATABASE}" "ALTER USER \"${DB_USERNAME}\" WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"
  fi

  log "granting all privileges for '${DB_USERNAME}' on '${DB_DATABASE}'"

  if [[ -f "${PG_DATA_DIR}/solidblocks_current_db_username" ]]; then
    local last_db_username=$(cat "${PG_DATA_DIR}/solidblocks_current_db_username")

    if [[ "${last_db_username}" != "${DB_USERNAME}" ]]; then
      log "reassigning ownerships from '${last_db_username}' to '${DB_USERNAME}'"

      psql_execute "${DB_DATABASE}" "REASSIGN OWNED BY \"${last_db_username}\" TO \"${DB_USERNAME}\""
      psql_execute "${DB_DATABASE}" "GRANT ALL PRIVILEGES ON DATABASE \"${DB_DATABASE}\" TO \"${DB_USERNAME}\""
      psql_execute "${DB_DATABASE}" "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO \"${DB_USERNAME}\""
      psql_execute "${DB_DATABASE}" "GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO \"${DB_USERNAME}\""
    fi
  fi

  echo "${DB_USERNAME}" > "${PG_DATA_DIR}/solidblocks_current_db_username"
}

function init_db() {
  log "initializing database instance"
  ${POSTGRES_BIN_DIR}/initdb --username="${DB_ADMIN_USERNAME}" --encoding=UTF8 --pwfile=<(uuidgen) -D "${PG_DATA_DIR}" || true

  cp -v /rds/config/postgresql.conf "${PG_DATA_DIR}/postgresql.conf"
  cp -v /rds/config/pg_hba.conf "${PG_DATA_DIR}/pg_hba.conf"

  # make sure we only listen public when DB is ready to go
  ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" start --options="-c listen_addresses=''"

  pgbackrest --config /rds/config/pgbackrest.conf --log-path=/rds/log  --log-level-console=info --stanza=${DB_DATABASE} stanza-create

  ensure_database
  ensure_user

  log "executing initial backup"
  pgbackrest_execute --log-level-console=info --type=full backup

  ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" stop
}

function pgbackrest_status_code() {
  PGBACKREST_INFO=$(pgbackrest_execute --output=json info)

  if [[ $(echo ${PGBACKREST_INFO} | jq length) -gt 0 ]]; then
    BACKUP_INFO=$(echo ${PGBACKREST_INFO} | jq ".[] | select(.name == \"${DB_DATABASE}\")")
    echo ${BACKUP_INFO} | jq -r '.status.code'
  else
    echo "99"
  fi
}

if [[ ! "$(ls -A ${PG_DATA_DIR})" ]]; then
  log "data dir is empty"

  if [[ $(pgbackrest_status_code) -eq 0 ]]; then

    log "restoring database from backup"
    # make sure we only listen public when DB is ready to go
    pgbackrest_execute --db-path=${PG_DATA_DIR} restore --recovery-option="recovery_end_command=/rds/bin/recovery_complete.sh"

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

    ensure_user

    ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" stop
  else
    init_db
  fi
else
  log "data dir is not empty"

  rm -f /rds/socket/*
  rm -f "${PG_DATA_DIR}/postmaster.pid"

  ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" start --options="-c listen_addresses=''"

  ensure_database
  ensure_user

  ${POSTGRES_BIN_DIR}/pg_ctl -D "${PG_DATA_DIR}" stop
fi

cp -v /rds/config/postgresql.conf "${PG_DATA_DIR}/postgresql.conf"
cp -v /rds/config/pg_hba.conf "${PG_DATA_DIR}/pg_hba.conf"

log "provisioning completed"
exec ${POSTGRES_BIN_DIR}/postgres -D "${PG_DATA_DIR}"
