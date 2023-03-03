#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

source "${DIR}/../../lib/python.sh"
source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/log.sh"
source "${DIR}/utils.sh"

log_divider_header "running python_ensure_venv"
echo "PyJWT==2.6.0" > "${TEMP_DIR}/requirements.txt"
(
  cd "${TEMP_DIR}"
  python_ensure_venv
  test_assert_file_exists "${TEMP_DIR}/venv/bin/python"
  test_assert_file_not_exists "${TEMP_DIR}/venv/bin/pytest"
)
log_divider_footer

log_divider_header "running python_ensure_venv again"
(
  cd "${TEMP_DIR}"
  python_ensure_venv
  test_assert_file_exists "${TEMP_DIR}/venv/bin/python"
  test_assert_file_not_exists "${TEMP_DIR}/venv/bin/pytest"
)
log_divider_footer

echo "pytest-testinfra==7.0.0" >> "${TEMP_DIR}/requirements.txt"

log_divider_header "running python_ensure_venv again with new requirement"
(
  cd "${TEMP_DIR}"
  python_ensure_venv
  test_assert_file_exists "${TEMP_DIR}/venv/bin/python"
  test_assert_file_exists "${TEMP_DIR}/venv/bin/pytest"
)
log_divider_footer

rm -rf "${TEMP_DIR}"
mkdir -p "${TEMP_DIR}"

log_divider_header "running python_ensure_venv with working dir as argument"
echo "PyJWT==2.6.0" > "${TEMP_DIR}/requirements.txt"
(
  python_ensure_venv "${TEMP_DIR}"
  test_assert_file_exists "${TEMP_DIR}/venv/bin/python"
  test_assert_file_not_exists "${TEMP_DIR}/venv/bin/pytest"
)
log_divider_footer

rm -rf "${TEMP_DIR}"
mkdir -p "${TEMP_DIR}"

log_divider_header "running python_ensure_venv with working dir as argument and different venv"
echo "PyJWT==2.6.0" > "${TEMP_DIR}/requirements.txt"
(
  python_ensure_venv "${TEMP_DIR}" "venv1"
  test_assert_file_exists "${TEMP_DIR}/venv1/bin/python"
  test_assert_file_not_exists "${TEMP_DIR}/venv1/bin/pytest"
)
log_divider_footer
