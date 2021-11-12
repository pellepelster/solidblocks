#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

function task_execute() {
  if [[ ! -f "${DIR}/venv/bin/activate" ]]; then
    task_bootstrap
  fi

  source "${DIR}/venv/bin/activate"

  export PYTHONPATH="${DIR}/ctuhl/lib/python"

  python "${DIR}/do.py" "$@"
}

function task_bootstrap {
  python3 -m venv "${DIR}/venv"
  source "${DIR}/venv/bin/activate"
  pip install --upgrade pip
  pip install wheel
  pip install -r "${DIR}/requirements.txt"
}

case ${1:-} in
  bootstrap) task_bootstrap ;;
  *) task_execute "$@" ;;
esac
