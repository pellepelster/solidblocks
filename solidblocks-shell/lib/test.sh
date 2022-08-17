#!/usr/bin/env bash

_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${_DIR}/colors.sh"
source "${_DIR}/utils.sh"

function test_assert_equals {
    local description=${1:-}
    local expected=${2:-}
    local actual=${3:-}

    if [[ "${expected}" == "${actual}" ]]; then
        echo -e "${COLOR_GREEN}${description} expected '${expected}' and got '${actual}'${COLOR_RESET}"
    else
        echo -e "${COLOR_RED}${description} expected '${expected}' but got '${actual}'${COLOR_RESET}"
        exit 1
    fi
}

function test_assert_matches {
    local description=${1:-}
    local expected=${2:-}
    local actual=${3:-}

    if [[ "${actual}" =~ "${expected}" ]]; then
        echo -e "${COLOR_GREEN}${description} expected '${expected}' and got '${actual}'${COLOR_RESET}"
    else
        echo -e "${COLOR_RED}${description} expected '${expected}' but got '${actual}'${COLOR_RESET}"
        exit 1
    fi
}

function test_assert_file_not_exists {
    local file=${1:-}

    if [[ -f "${file}" ]]; then
        echo -e "${COLOR_RED}file ${file} found${COLOR_RESET}"
        exit 1
    else
        echo -e "${COLOR_GREEN}file ${file} not found${COLOR_RESET}"
    fi
}

function test_assert_file_exists {
    local file=${1:-}

    if [[ -f "${file}" ]]; then
        echo -e "${COLOR_GREEN}file ${file} found${COLOR_RESET}"
    else
        echo -e "${COLOR_RED}file ${file} not found${COLOR_RESET}"
        exit 1
    fi
}

function test_assert_json {
    ensure_command "jq"

    local description=${1:-}
    local expected=${2:-}
    local path=${3:-}
    local json=${4:-}

    local actual=$(echo "${json}" | jq -rc ${path})
    assert_equals "${description}: '${path}'" "${expected}" "${actual}" 
}

function test_assert_xml {
    local description=${1:-}
    local expected=${2:-}
    local path=${3:-}
    local xml=${4:-}

    local actual=$(echo "${xml}" | xmllint ${path})
    assert_equals "${description}: '${path}'" "${expected}" "${actual}"
}