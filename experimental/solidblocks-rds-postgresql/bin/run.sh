#!/usr/bin/env bash

set -eu

echo "========================================"
echo "solidblocks-rds-postgres"
echo "========================================"

gomplate --input-dir /rds/templates/config/ --output-map='/rds/config/{{ .in | strings.ReplaceAll ".template" "" }}'

if [[ -z "${DB_DATABASE:-}" ]]; then
  echo "DB_DATABASE not set"
  exit 1
fi

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "DB_PASSWORD not set"
  exit 1
fi

if [[ -z "${DB_USERNAME:-}" ]]; then
  echo "DB_USERNAME not set"
  exit 1
fi

if [[ -z "${DB_SSH_PRIVATE_KEY:-}" ]]; then
  echo "DB_SSH_PRIVATE_KEY not set"
  exit 1
fi

if [[ -z "${BACKUP_HOST_PUBLIC_KEY:-}" ]]; then
  echo "BACKUP_HOST_PUBLIC_KEY not set"
  exit 1
fi

POSTGRES_BASE_DIR="/usr/libexec/postgresql14"
POSTGRES_BIN_DIR="${POSTGRES_BASE_DIR}"

mkdir -p "${DATA_DIR}"
mkdir -p "${BACKUP_DIR}"

mkdir -p ".ssh" || true
chmod -R 750 ".ssh/"

echo "${DB_SSH_PRIVATE_KEY}" > ".ssh/id_ed25519"
chmod -R 600 ".ssh/id_ed25519"

echo "${BACKUP_HOST}    ${BACKUP_HOST_PUBLIC_KEY}" > ".ssh/known_hosts"
chmod -R 600 ".ssh/known_hosts"

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

  pgbackrest --config /rds/config/pgbackrest.conf --log-path=/rds/log --stanza=${DB_DATABASE} stanza-create
  #sleep 9999999

  if [[ $(psql_count "SELECT count(datname) FROM pg_database WHERE datname = '${DB_DATABASE}';") == "0" ]]; then
    echo "creating database '${DB_DATABASE}'"
    psql_execute "CREATE DATABASE ${DB_DATABASE}"
  fi

  if [[ $(psql_count "SELECT count(u.usename) FROM pg_catalog.pg_user u WHERE u.usename = '${DB_USERNAME}';") == "0" ]]; then
    echo "creating user '${DB_USERNAME}'"
    psql_execute "CREATE USER ${DB_USERNAME} WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"
  fi

  echo "granting all privileges for '${DB_USERNAME}' on '${DB_DATABASE}'"
  psql_execute "GRANT ALL PRIVILEGES ON DATABASE ${DB_DATABASE} TO ${DB_USERNAME}"

  echo "executing initial backup"
  echo "========================================"
  #pgbackrest_execute --log-level-console=info backup
  echo "========================================"

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
  echo "data dir is empty"

  if [[ $(pgbackrest_status_code) -eq 0 ]]; then

    echo "========================================"
    echo "restoring database from backup"
    # make sure we only listen public when DB is ready to go
    pgbackrest_execute --db-path=${DATA_DIR} restore --recovery-option="recovery_end_command=/rds/bin/recovery_complete.sh"
    echo "========================================"

    sleep 5

    echo "========================================"
    echo "starting db for recovery"
    ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" start --options="-c listen_addresses=''"
    echo "========================================"

    while [[ -f /tmp/recovery_complete ]]; do
      echo "waiting for recovery completion"
      sleep 5
    done

    until [[ "$(psql_execute 'SELECT pg_is_in_recovery();' | tr -d '[:space:]')" == "f" ]]; do
      echo "waiting for server to be ready"
      sleep 5
    done

    echo "setting password for '${DB_USERNAME}'"
    psql_execute "ALTER USER ${DB_USERNAME} WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"

    ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" stop
  else
    init_db
  fi
else
  echo "data dir is not empty"

  rm -f /rds/socket/*
  rm -f "${DATA_DIR}/postmaster.pid"

  ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" start --options="-c listen_addresses=''"
  echo "setting password for '${DB_USERNAME}'"
  psql_execute "ALTER USER ${DB_USERNAME} WITH ENCRYPTED PASSWORD '${DB_PASSWORD}'"
  ${POSTGRES_BIN_DIR}/pg_ctl -D "${DATA_DIR}" stop
fi

echo "starting postgres db"

exec ${POSTGRES_BIN_DIR}/postgres -D "${DATA_DIR}"
