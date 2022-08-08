---
title: Installation
weight: 10
disableToc: true
---

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/snapshot/solidblocks-shell-snapshot.zip > solidblocks-shell-snapshot.zip
unzip solidblocks-shell-snapshot.zip
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

