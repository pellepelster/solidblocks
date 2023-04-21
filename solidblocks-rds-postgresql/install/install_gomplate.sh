#!/usr/bin/env bash

set -eu

GOMPLATE_VERSION=3.11.5

declare -A GOMPLATE_CHECKSUMS
GOMPLATE_CHECKSUMS[amd64]="eb225db8a40758f67a2af0a1f5e2fd78354fdac5e8be095d536bdebcc7a5a432"
GOMPLATE_CHECKSUMS[armv7]="a5b0affd54abd611aa0cc723d10aa75460dad87861672cdac59375fee7b47793"
GOMPLATE_CHECKSUMS[arm64]="7b45a64083a1e71b76881541db1554404a391cc1d6942a2a25f26614b069fcaf"

function go_arch() {
  case $(uname -m) in
    armv7l) echo "armv7" ;;
    aarch64) echo "arm64" ;;
    x86_64) echo "amd64" ;;
    *) echo "unknown" ;;
  esac
}

curl -L https://github.com/hairyhenderson/gomplate/releases/download/v${GOMPLATE_VERSION}/gomplate_linux-$(go_arch)-slim -o /usr/bin/gomplate
echo "${GOMPLATE_CHECKSUMS[$(go_arch)]}  /usr/bin/gomplate" | sha256sum -c
chmod +x /usr/bin/gomplate