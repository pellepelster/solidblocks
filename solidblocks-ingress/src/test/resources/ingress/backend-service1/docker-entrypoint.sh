#!/bin/sh
# vim:sw=4:ts=4:et

set -e

echo "${SERVER_CERT}" > /server.cert
echo "${SERVER_KEY}" > /server.key
mkdir -p /etc/nginx/html
echo "Hello World!" > /etc/nginx/html/test.txt

exec "$@"