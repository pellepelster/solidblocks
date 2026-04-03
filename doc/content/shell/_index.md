+++
title = "Shell"
weight = 10
description = "Reusable shell functions for infrastructure automation and developer experience"
overviewGroup = "shell"
faIcon = "fa-terminal"
+++

Reusable shell functions for infrastructure automation and developer experience

## Installation

Releases are viable via [https://github.com/pellepelster/solidblocks/releases](https://github.com/pellepelster/solidblocks/releases), for direct usage in shell-scripts the following helper function provides automatic installation


```shell
{{% include-version "/snippets/blcks-shell-bootstrap-solidblocks-v%s.sh" %}}
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

## Functions

{{% children description="true" %}}

## Kotlin Abstraction

Building up on the basic shell function, a Kotlin abstraction serves as type-safe interface to those functions. This is mainly intended as building blocks for non-interactive Cloud-Init shell scripts.

```kotlin
val script = ShellScript()

/**
inline sources are directly included in the rendered script
 */
script.addInlineSource(StorageLibrary)
script.addInlineSource(DockerLibrary)
script.addInlineSource(AptLibrary)

/**
library sources are written to ShellScript.LIB_SOURCES_PATH and sources from there
 */
script.addLibSources(PackageLibrary)

script.addCommand(PackageLibrary.UpdateRepositories())
script.addCommand(PackageLibrary.InstallPackage("jq"))
script.addCommand(DockerLibrary.InstallDebian())

val rawScript = script.render()
```


All functions are tested on the following distributions

* Amazon Linux 2
* Debian 10
* Debian 11
* Debian 12
* Ubuntu 20.04
* Ubuntu 22.04
* Ubuntu 24.04

## Kitchen Sink Usage Example

```shell
{{% include-version "/snippets/blcks-shell-kitchen-sink-v%s.sh" %}}
```
