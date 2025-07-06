+++
title = "Python"
weight = 15
description = "Reusable Python functions for infrastructure automation and developer experience"
overviewGroup = "python"
faIcon = "fa-terminal"
+++

Reusable Python functions for CLI infrastructure automation and developer experience.

{{% children description="true" %}}

## Kitchen Sink Usage Example

**requirements.txt**
```python
https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/solidblocks_do-{{% env "SOLIDBLOCKS_VERSION" %}}-py3-none-any.whl
click==8.1.7
```

**do**
```shell
#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"
export VENV_DIR="${DIR}/.venv"

function python_hash_requirements() {
  local dir="${1:-.}"
  sha256sum < "${dir}/requirements.txt" | cut -f 1 -d " "
}

function python_ensure_venv() {
  local dir="${1:-.}"
  local venv_dir="${dir}/${2:-.venv}"

  mkdir -p "${venv_dir}"
  local requirements_hash_file="${venv_dir}/requirements.sha256"

  if [[ ! -f "${requirements_hash_file}" ]]; then
    echo "random" | sha256sum > "${requirements_hash_file}"
  fi

  if [[ "$(python_hash_requirements "${dir}")" != "$(cat "${requirements_hash_file}")" ]]; then
    python3 -m venv "${venv_dir}"
    "${venv_dir}/bin/pip" install -r "${dir}/requirements.txt"
    python_hash_requirements "${dir}" > "${requirements_hash_file}"
  fi
}

python_ensure_venv "${DIR}"
export PYTHONUNBUFFERED=1
"${VENV_DIR}/bin/python" "${DIR}/do.py" $@
```

**do.py**
```shell
import click
from solidblocks_do import log_hint, command_run

@click.group()
def cli():
    pass


@click.command("command1")
def cli_command1():
    """run some command"""

    log_hint("running cli_command1")
    if not command_run(["command1"]): raise click.Abort()


cli.add_command(cli_command1)


if __name__ == '__main__':
    cli()
```

**usage**
```shell
./do --help
Usage: do.py [OPTIONS] COMMAND [ARGS]...

Options:
  --help  Show this message and exit.

Commands:
  command1  run some command
```