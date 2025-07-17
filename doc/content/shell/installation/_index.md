---
title: Installation
description: Installation instructions
---

Releases are viable via [https://github.com/pellepelster/solidblocks/releases](https://github.com/pellepelster/solidblocks/releases), for direct usage in shell-scripts the following helper function provides automatic installation


```shell
{{% includef "/snippets/shell-bootstrap-solidblocks-%s.sh" %}}
```

A full example that you can use as a skeleton for your own scripts

```shell
{{% includef "/snippets/shell-minimal-skeleton-%s.sh" %}}
```

After download and extraction the different components can be sourced in via


```shell
DIR="$(cd "$(dirname "$0")" ; pwd -P)"

source "${DIR}/.solidblocks-shell/software.sh"

software_ensure_terraform
```

{{% notice info %}}
Note than when sourcing in the different files, all dependencies are also automatically loaded. E.g. when using the software helpers it will automatically also source in `download.sh` and `file.sh` because it needs functions from this file.
{{% /notice %}}
