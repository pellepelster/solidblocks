#!/usr/bin/env bash

set -eux

systemctl reload sshd

while true; do
	echo "node management ping"
	sleep 30
done
