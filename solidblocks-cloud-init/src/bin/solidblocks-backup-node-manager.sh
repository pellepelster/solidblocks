#!/usr/bin/env bash

set -eux

systemctl reload sshd

while true; do
	echo "solidblocks backup node manager ping"
	sleep 30
done
