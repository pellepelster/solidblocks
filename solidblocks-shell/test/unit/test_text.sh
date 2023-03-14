#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../../lib/text.sh"

echo "${FORMAT_DIM}Dim${FORMAT_RESET}"
echo "${FORMAT_UNDERLINE}Underline${FORMAT_RESET}"
echo "${FORMAT_BOLD}Bold${FORMAT_RESET}"
echo "${COLOR_RED}Red${COLOR_RESET}"
echo "${COLOR_GREEN}Green${COLOR_RESET}"
echo "${COLOR_YELLOW}Yellow${COLOR_RESET}"
echo "${COLOR_BLACK}Black${COLOR_RESET}"
echo "${COLOR_BLUE}Blue${COLOR_RESET}"
echo "${COLOR_MAGENTA}Magenta${COLOR_RESET}"
echo "${COLOR_CYAN}Cyan${COLOR_RESET}"
echo "${COLOR_WHITE}White${COLOR_RESET}"


echo ""
echo "                                             Normal"
echo "\${FORMAT_DIM}Dim\${FORMAT_RESET}:             ${FORMAT_DIM}Dim${FORMAT_RESET}"
echo "\${FORMAT_UNDERLINE}Underline\${FORMAT_RESET}: ${FORMAT_UNDERLINE}Underline${FORMAT_RESET}"
echo "\${FORMAT_BOLD}Bold\${FORMAT_RESET}:           ${FORMAT_BOLD}Bold${FORMAT_RESET}"
echo "\${COLOR_RED}Red\${COLOR_RESET}:               ${COLOR_RED}Red${COLOR_RESET}"
echo "\${COLOR_GREEN}Green\${COLOR_RESET}:           ${COLOR_GREEN}Green${COLOR_RESET}"
echo "\${COLOR_YELLOW}Yellow\${COLOR_RESET}:         ${COLOR_YELLOW}Yellow${COLOR_RESET}"
echo "\${COLOR_BLACK}Black\${COLOR_RESET}:           ${COLOR_BLACK}Black${COLOR_RESET}"
echo "\${COLOR_BLUE}Blue\${COLOR_RESET}:             ${COLOR_BLUE}Blue${COLOR_RESET}"
echo "\${COLOR_MAGENTA}Magenta\${COLOR_RESET}:       ${COLOR_MAGENTA}Magenta${COLOR_RESET}"
echo "\${COLOR_CYAN}Cyan\${COLOR_RESET}:             ${COLOR_CYAN}Cyan${COLOR_RESET}"
echo "\${COLOR_WHITE}White\${COLOR_RESET}:           ${COLOR_WHITE}White${COLOR_RESET}"
