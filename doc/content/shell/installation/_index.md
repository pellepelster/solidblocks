---
title: Installation
weight: 10
disableToc: true
---

```shell
SOLIDBLOCKS_SHELL_VERSION="TEMPLATE_SOLIDBLOCKS_SHELL_VERSION"
SOLIDBLOCKS_SHELL_CHECKSUM="TEMPLATE_SOLIDBLOCKS_SHELL_CHECKSUM"

curl -L "https://github.com/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_SHELL_VERSION}/solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" > "solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip"
echo "${SOLIDBLOCKS_SHELL_CHECKSUM}  solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" | sha256sum -c
unzip "solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip"
```

After download and extraction the different components can be sourced in via


```shell
DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/software.sh"

software_ensure_terraform
```

{{% notice info %}}
Note than when sourcing in the different files, all dependencies are also automatically loaded. E.g. when using the software helpers it will automatically also source in `download.sh` and `file.sh` because it needs functions from this file.
{{% /notice %}}

