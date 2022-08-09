---
title: Download
weight: 20
disableToc: true
---

Secure and reliable data retrieval from remote servers

## Functions

### `download_and_verify_checksum(url, target_file, checksum)` {#download_and_verify_checksum}

Downloads the file given by `${url}` to `${target_file}` and verifies if the downloaded file matches the checksum `${checksum}`. If a file is already present at `${target}` download is skipped.

```shell
source "download.sh"

download_and_verify_checksum "https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip" "${TEMP_DIR}/download_and_verify_checksum_$$/file.zip" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
```

