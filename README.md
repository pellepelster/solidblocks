[![solidblocks](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml/badge.svg)](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml)

# Solidblocks

Solidblocks is a library of reusable components for infrastructure  automation, documentation is available here  https://pellepelster.github.io/solidblocks/

## Shell

Reusable shell functions for infrastructure automation ([documentation](https://pellepelster.github.io/solidblocks/shell/))

All functions are tested on the following distributions

* Amazon Linux 2
* Debian 10
* Debian 11
* Ubuntu 20.04
* Ubuntu 22.04

### Usage example

#### Installation
```
curl -L https://github.com/pellepelster/solidblocks/releases/download/v0.0.60/solidblocks-shell-v0.0.60.zip > solidblocks-shell-v0.0.60.zip
unzip solidblocks-shell-v0.0.60.zip
```

#### Create Script
**example.sh**
```
#!/usr/bin/env bash

source "solidblocks-shell/software.sh"

software_ensure_terraform
software_set_export_path

terraform version
```

#### Run Script

```
$ ./example.sh

Usage: terraform [-version] [-help] <command> [args]

The available commands for execution are listed below.
The most common, useful commands are shown first, followed by
less common or more advanced commands. If you're just getting
started with Terraform, stick with the common commands. For the
other commands, please read the help and docs before usage.

Common commands:
    apply              Builds or changes infrastructure
    console            Interactive console for Terraform interpolations
    [...]
    version            Prints the Terraform version
    workspace          Workspace management

All other commands:
    0.12upgrade        Rewrites pre-0.12 module source code for v0.12
    [...]
    state              Advanced state management

```
