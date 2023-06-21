#!/usr/bin/env bash

set -eu

GOMPLATE_VERSION=3.11.5

declare -A GOMPLATE_CHECKSUMS
GOMPLATE_CHECKSUMS[amd64]="16f6a01a0ff22cae1302980c42ce4f98ca20f8c55443ce5a8e62e37fc23487b3"
GOMPLATE_CHECKSUMS[armv7]="c819e335c6c05a79e42e5d151ee8c0cccd5a7c2d189fed90fab05ab4cc71e607"
GOMPLATE_CHECKSUMS[arm64]="fd980f9d233902e50f3f03f10ea65f36a2705385358a87aa18b19fb7cdf54c1d"

function go_arch() {
  case $(uname -m) in
    armv7l) echo "armv7" ;;
    aarch64) echo "arm64" ;;
    x86_64) echo "amd64" ;;
    *) echo "unknown" ;;
  esac
}

curl -L https://github.com/hairyhenderson/gomplate/releases/download/v${GOMPLATE_VERSION}/gomplate_linux-$(go_arch) -o /usr/bin/gomplate
echo "${GOMPLATE_CHECKSUMS[$(go_arch)]}  /usr/bin/gomplate" | sha256sum -c
chmod +x /usr/bin/gomplate

# verify install has worked
#gomplate --version