---
title: Command
weight: 50
description: Run CLI commands
---

Helper to assert and run other CLI commands and work with its output.

## Functions

### `command_ensure_exists(command)` {#command_ensure_exists}

Verifies that a command is available, and logs a message in case it is missing. 

```python
from blcks_do.command import command_ensure_exists

if not command_ensure_exists('terraform'):
    pass
```


### `command_run(command, env=None, workdir=None, shell=True)` {#command_run}

Runs a command and prints to full command line, as well as the commands output.

```python
from blcks_do.command import command_run

if command_run(['env', '--debug'], env={"SOME_ENV": "foo-bar"}, workdir="/tmp"):
    pass
```

**output**
```
▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
running command 'env --debug' in '/tmp'
────────────────────────────────────────────────────────────────────────────────
USER=pelle
[...]
SOME_ENV=foo-bar
▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
```

### `command_exec_json(command, env=None, workdir=None)` {#command_exec_json}

Executes a command and parses the output as JSON.

```python
from blcks_do.command import command_exec_json

result = command_exec_json('some_json_command.sh')

if result is not None:
    print(result['attribute1'])
```
