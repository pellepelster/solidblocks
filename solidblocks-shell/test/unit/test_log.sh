#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/../../lib/log.sh"

echo ""
log_echo_error "error message"

echo ""
log_info "info message"
log_success "success message"
log_warning "warning message"
log_debug "debug message"
log_error "error message"

echo ""
#log_die "fatal message"

echo ""
log_divider_header "directory content"
ls -lsa
log_divider_footer
