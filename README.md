[![solidblocks](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml/badge.svg)](https://github.com/pellepelster/solidblocks/actions/workflows/pipeline.yml)

# Solidblocks

Solidblocks is a library of reusable components for infrastructure  automation, documentation is available here  https://pellepelster.github.io/solidblocks/

## Shell

Reusable shell functions for infrastructure automation ([documentation](https://pellepelster.github.io/solidblocks/shell/))

**solidblocks.sh**
```
#!/usr/bin/env bash

source "software.sh"

software_ensure_terraform
software_set_export_path

terraform version
```

```
$ ./solidblocks.sh

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
