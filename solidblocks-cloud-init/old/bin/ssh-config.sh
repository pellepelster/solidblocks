#!/bin/bash

set -eux

systemctl reload sshd

while true; do
	echo "ssh ping"
	sleep 5
done
