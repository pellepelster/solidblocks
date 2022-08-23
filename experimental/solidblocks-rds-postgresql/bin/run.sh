#!/usr/bin/env bash

set -eu

function log() {
  echo "[solidblocks-rds-postgresql] $*"
}

function ensure_environment_variables() {
  # verify common needed variables
  for var in $@; do
    if [[ -z "${!var:-}" ]]; then
      log "'$var' is empty or not set"
      exit 1;
    fi
  done
}

ensure_environment_variables DB_DATABASE DB_PASSWORD DB_USERNAME

ensure_environment_variables DB_BACKUP_S3_CA_PUBLIC_KEY DB_BACKUP_S3_HOST DB_BACKUP_S3_BUCKET DB_BACKUP_S3_ACCESS_KEY DB_BACKUP_S3_SECRET_KEY

log "starting..."

if [[ -n "${DB_BACKUP_S3_CA_PUBLIC_KEY:-}" ]]; then
mkdir -p /rds/certificates
echo -n "${DB_BACKUP_S3_CA_PUBLIC_KEY}" | base64 -d > /rds/certificates/ca.pem
fi


if ! mount | grep "${LOCAL_STORAGE_DIR}"; then
    log "storage dir '${LOCAL_STORAGE_DIR}' not mounted"
    exit 1
fi

gomplate --input-dir /rds/templates/config/ --output-map='/rds/config/{{ .in | strings.ReplaceAll ".template" "" }}'
gomplate --input-dir /rds/templates/bin/ --output-map='/rds/bin/{{ .in | strings.ReplaceAll ".template" "" }}'

POSTGRES_BASE_DIR="/usr/libexec/postgresql14"
POSTGRES_BIN_DIR="${POSTGRES_BASE_DIR}"

mkdir -p "${DATA_DIR}"
mkdir -p "${BACKUP_DIR}"

chmod 700 "${DATA_DIR}"

function psql_execute() {
  local query=${1:-}
  psql -h /rds/socket postgres --field-separator-zero --record-separator-zero --tuples-only --quiet -c "${query}"
}

function pgbackrest_execute() {
  pgbackrest --config /rds/config/pgbackrest.conf --log-path=/rds/log --stanza=${DB_DATABASE} "$@"
}

function psql_count() {
  psql_execute "$@" | tr -d '[:space:]'
}

function init_db() {
  log "initializing database instance"
  ${POSTGRES_BIN_DIR}/initdb --username="rds" --encoding=UTF8 --pwfile=<(echo "${DB_PASSWORD}") -D "${DATA_DIR}" || true
  cp -v /rds/config/postgresql.conf "${DATA_DIR}/postgresql.conf"
  cp -v /rds/config/pg_hba.conf "${DATA_DIR}/pg_hba.conf"

  # make sure we only listen public when DB is ready to go
  ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" start --options="-c listen_addresses=''"

  mkdir -p "${BACKUP_DIR}"

  if [[ $(pgbackrest_status_code) -gt 0 ]]; then

    if [[ "$(ls -A ${BACKUP_DIR})" ]]; then
      local invalid_backups_dir="${BACKUP_DIR}/.invalid_backups_$(date +%Y%m%d%H%M%S)"
      mkdir -p ${invalid_backups_dir}
      mv ${BACKUP_DIR}/* ${invalid_backups_dir}
    fi
  fi

  pgbackrest --config /rds/config/pgbackrest.conf --log-path=/rds/log  --log-level-console=info --stanza=${DB_DATABASE} stanza-create

  if [[ $(psql_count "SELECT count(datname) FROM pg_database WHERE datname = '${DB_DATABASE}';") == "0" ]]; then
    log "creating database '${DB_DATABASE}'"
    psql_execute "CREATE DATABASE ${DB_DATABASE}"
  fi

  if [[ $(psql_count "SELECT count(u.usename) FROM pg_catalog.pg_user u WHERE u.usename = '${DB_USERNAME}';") == "0" ]]; then
    log "creating user '${DB_USERNAME}'"
    psql_execute "CREATE USER ${DB_USERNAME} WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"
  fi

  log "granting all privileges for '${DB_USERNAME}' on '${DB_DATABASE}'"
  psql_execute "GRANT ALL PRIVILEGES ON DATABASE ${DB_DATABASE} TO ${DB_USERNAME}"

  log "executing initial backup"
  pgbackrest_execute --log-level-console=info --type=full backup

  ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" stop
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

if [[ ! "$(ls -A ${DATA_DIR})" ]]; then
  log "data dir is empty"

  if [[ $(pgbackrest_status_code) -eq 0 ]]; then

    log "restoring database from backup"
    # make sure we only listen public when DB is ready to go
    pgbackrest_execute --db-path=${DATA_DIR} restore --recovery-option="recovery_end_command=/rds/bin/recovery_complete.sh"

    sleep 5

    log "starting db for recovery"
    ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" start --options="-c listen_addresses=''"

    while [[ -f /tmp/recovery_complete ]]; do
      log "waiting for recovery completion"
      sleep 5
    done

    until [[ "$(psql_execute 'SELECT pg_is_in_recovery();' | tr -d '[:space:]')" == "f" ]]; do
      log "waiting for server to be ready"
      sleep 5
    done

    log "setting password for '${DB_USERNAME}'"
    psql_execute "ALTER USER ${DB_USERNAME} WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"

    ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" stop
  else
    init_db
  fi
else
  log "data dir is not empty"

  rm -f /rds/socket/*
  rm -f "${DATA_DIR}/postmaster.pid"

  ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" start --options="-c listen_addresses=''"
  log "setting password for '${DB_USERNAME}'"
  psql_execute "ALTER USER ${DB_USERNAME} WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"
  ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" stop
fi

log "provisioning completed"
exec ${POSTGRES_BIN_DIR}/postgres -D "${DATA_DIR}"
