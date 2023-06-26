#!/usr/bin/env bash

set -eu

find /storage/**/archive/* -name '*.gz' -exec od -A none --format x1 --read-bytes 8 {} \;