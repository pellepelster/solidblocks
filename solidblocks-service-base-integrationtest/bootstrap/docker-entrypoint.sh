#!/bin/sh
# vim:sw=4:ts=4:et

set -e

mkdir -p /etc/nginx/html
echo "Hello World!" > /etc/nginx/html/test.txt

exec "$@"