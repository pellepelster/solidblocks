#!/usr/bin/env bash

set -eu

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

#env | grep "DB_DATABASE_"

function databases() {

  for database_var in "${!DB_DATABASE_@}"; do

    local database="${database_var#"DB_DATABASE_"}"

    if [[ -z "${database}" ]]; then
      echo "provided database name was empty"
      continue
    fi

    local database_user_var="DB_USERNAME_${database}"
    local username="${!database_user_var:-}"
    if [[ -z "${username}" ]]; then
      echo "no username provided for database '${database}'"
      continue
    fi


    local database_password_var="DB_PASSWORD_${database}"
    local password="${!database_password_var:-}"
    if [[ -z "${password}" ]]; then
      echo "no password provided for database '${database}'"
      continue
    fi

    echo "ensuring database '${database}' with user '${username}'"

  done

}

databases