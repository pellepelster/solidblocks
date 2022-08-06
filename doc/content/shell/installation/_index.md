---
title: Installation
weight: 10
disableToc: true
---

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/SOLIDBLOCKS_VERSION/solidblocks-shell-SOLIDBLOCKS_VERSION.zip > solidblocks-shell-SOLIDBLOCKS_VERSION.zip
unzip solidblocks-shell-SOLIDBLOCKS_VERSION.zip
```

After download and extraction the different components can be sourced in via 


```shell
DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/solidblocks-shell/download.sh"
source "${DIR}/solidblocks-shell/software.sh"
source "${DIR}/solidblocks-shell/file.sh"
```

