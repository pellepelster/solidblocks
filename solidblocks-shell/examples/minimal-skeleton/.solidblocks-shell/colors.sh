#!/usr/bin/env bash

function tput_wrapper() {
  if [[ -t 0 ]]; then
    tput $@
  fi
}

COLOR_RED=$(tput_wrapper -Txterm-256color setaf 1)
COLOR_GREEN=$(tput_wrapper -Txterm-256color setaf 2)
COLOR_YELLOW=$(tput_wrapper -Txterm-256color setaf 3)
COLOR_BLACK=$(tput_wrapper -Txterm-256color setaf 0)
COLOR_BLUE=$(tput_wrapper -Txterm-256color setaf 4)
COLOR_MAGENTA=$(tput_wrapper -Txterm-256color setaf 5)
COLOR_CYAN=$(tput_wrapper -Txterm-256color setaf 6)
COLOR_WHITE=$(tput_wrapper -Txterm-256color setaf 15)

COLOR_RESET=$(tput_wrapper -Txterm-256color sgr0)

FORMAT_BOLD=$(tput_wrapper bold)

FORMAT_RESET=${COLOR_RESET}

