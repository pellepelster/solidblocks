
function tput_wrapper() {
  if [[ -t 0 ]]; then
    tput $@
  fi
}

TEST_COLOR_RED=$(tput_wrapper -Txterm-256color setaf 1)
TEST_COLOR_GREEN=$(tput_wrapper -Txterm-256color setaf 2)
TEST_COLOR_RESET=$(tput_wrapper -Txterm-256color sgr0)

function test_assert_equals {
    local description=${1:-}
    local expected=${2:-}
    local actual=${3:-}

    if [[ "${expected}" == "${actual}" ]]; then
        echo "${TEST_COLOR_GREEN}${description} expected '${expected}' and got '${actual}'${TEST_COLOR_RESET}"
    else
        echo "${TEST_COLOR_RED}${description} expected '${expected}' but got '${actual}'${TEST_COLOR_RESET}"
        exit 1
    fi
}

function test_assert_matches {
    local description=${1:-}
    local expected=${2:-}
    local actual=${3:-}

    if [[ "${actual}" =~ "${expected}" ]]; then
        echo "${TEST_COLOR_GREEN}${description} expected '${expected}' and got '${actual}'${TEST_COLOR_RESET}"
    else
        echo "${TEST_COLOR_RED}${description} expected '${expected}' but got '${actual}'${TEST_COLOR_RESET}"
        exit 1
    fi
}

function test_assert_file_not_exists {
    local file=${1:-}

    if [[ -f "${file}" ]]; then
        echo "${TEST_COLOR_RED}file ${file} found${TEST_COLOR_RESET}"
        exit 1
    else
        echo "${TEST_COLOR_GREEN}file ${file} not found${TEST_COLOR_RESET}"
    fi
}

function test_assert_file_exists {
    local file=${1:-}

    if [[ -f "${file}" ]]; then
        echo "${TEST_COLOR_GREEN}file ${file} found${TEST_COLOR_RESET}"
    else
        echo "${TEST_COLOR_RED}file ${file} not found${TEST_COLOR_RESET}"
        exit 1
    fi
}

function test_assert_json {
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