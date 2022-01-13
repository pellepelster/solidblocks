#!/bin/sh
# vim:sw=4:ts=4:et

set -e

echo "${SERVER_CERT}" > /server.cert
echo "${SERVER_KEY}" > /server.key
echo "${CLIENT_CA_CERT}" > /client_ca.cert

mkdir -p /etc/nginx/html
echo "Hello World!" > /etc/nginx/html/test.txt


cat /server.cert
cat /server.key
cat /client_ca.cert

exec "$@"