---
title: Terraform/Tofu
weight: 30
description: Wrappers to run Terraform and Tofu commands
---

Wrappers to run Terraform and Tofu commands

## Functions

### `[terraform|tofu]_ensure(min_version=None)` {#terraform_ensure}

Ensure that the terraform/tofu executable present, and optionally makes sure, that it is a least `min_version`.

```python
from solidblocks_do.terraform import terraform_ensure

if terraform_ensure('1.1.0'):
    pass
```

### `[terraform|tofu]_init(path, args=[], env=None)` {#terraform_init}

Runs terraform/tofu init in `path` with extra arguments `args` and environment `env`.   

```python
from solidblocks_do.terraform import terraform_init

if terraform_init('path/to/tf/files', ['--upgrade']):
    pass
```

### `[terraform|tofu]_apply(path, apply=False, args=[], env=None)` {#terraform_init}

Runs terraform/tofu apply in `path` with extra arguments `args` and environment `env`. If `apply` is `True` apply will be started with `-auto-approve`.   

```python
from solidblocks_do.terraform import terraform_apply

if terraform_apply('path/to/tf/files'):
    pass
```

### `[terraform|tofu]_has_output(path, output)` {#terraform_has_output}

Returns `True` if terraform/tofu at `path` has output named `output`.   

```python
from solidblocks_do.terraform import terraform_has_output

if terraform_has_output('path/to/tf/files', 'output1'):
    pass
```

### `[terraform|tofu]_get_output(path, output)` {#terraform_get_output}

Returns content of `output` from terraform/tofu at `path`.   

```python
from solidblocks_do.terraform import terraform_get_output

result = terraform_get_output('path/to/tf/files', 'output1')

if result is not None:
    pass
```

### `[terraform|tofu]_print_output(path)` {#terraform_print_output}

Prints terraform/tofu output from `path` as key value pairs.   

```python
from solidblocks_do.terraform import terraform_print_output

terraform_print_output('path/to/tf/files')
```

**output**
```
▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
tofu output for '/home/pelle/git/solidblocks/solidblocks-do-python/tests/tofu'
──────────────────────────────────────────────────────────────────────────────
foo = "bar"
random = "cnAhlSz9xVHZ00vF"
▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
```
