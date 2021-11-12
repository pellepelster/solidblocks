#!/usr/bin/env bash

set -eux

# TODO(pelle): migrate to systemd path hooks
if [[ -f "/etc/systemd/system/consul-server.service" ]]; then
    systemctl reload consul-server.service
fi

#if [[ -f "/etc/postfix/main.cf" ]]; then
#  postmap /etc/postfix/smtp_auth
#  systemctl reload postfix
#fi

while true; do
	echo "node management ping"
	sleep 5
done
