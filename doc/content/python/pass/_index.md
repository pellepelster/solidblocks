---
title: pass
weight: 40
description: Wrapper functions for the pass password manager
---

Wrapper functions for the [pass](https://www.passwordstore.org/) password manager.

## Common Arguments

### `secret_store`

Directory where the `pass` secrets are stored, defaults to `<current working directory>/secrets` if not provided.

### `path`

Path to the secret inside the password store. All functions automatically use the path to check for an environment
variable, to allow for overriding in CI environments where access to the password store may not be desirable.

| path                    | environment variable    |
|-------------------------|-------------------------|
| secret1                 | SECRET1                 |
| nested/secret/password2 | NESTED_SECRET_PASSWORD2 |
| secret-with-dashes3     | SECRET_WITH_DASHES3     |

``

### `env_name`

Override the environment variable named derived from `path`.

## Functions

### `pass_has_secret(path, secret_store=None, env_name=None)` {#pass_has_secret}

Returns `True` if secret `path` exists in `secret_store` or is injected via environment variable derived from path or `env_name`.

```python
from solidblocks_do.secrets_pass import pass_has_secret

if pass_has_secret('secret1', '/path/to/secret/store'):
    pass
```

### `pass_get_secret(path, secret_store=None, env_name=None)` {#pass_get_secret}

Returns value of secret at `path` inside `secret_store` or from environment variable derived from path (or `env_name`).

```python
from solidblocks_do.secrets_pass import pass_get_secret

secret = pass_get_secret('secret1', '/path/to/secret/store')

if secret is not None:
    pass
```

### `pass_temp_file(path, secret_store=None, env_name=None)` {#pass_temp_file}

Stores value of secret at `path` from `secret_store` in a temporary file, that will get deleted when the `pass_temp_file` context is closed.

```python
from solidblocks_do.secrets_pass import pass_temp_file

with pass_temp_file('some/password', '/path/to/secret/store') as temp_secret_file:
    secret = open(temp_secret_file, 'r').read()
```

