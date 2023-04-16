---
title: Python
weight: 80
---

Wrappers and helpers for Python

## Functions

### `python_ensure_venv([dir=.], [venv_dir=venv])` {#python_ensure_venv}

Bootstraps a new [Python Virtual Env](https://docs.python.org/3/library/venv.html) in the current directory, assuming it contains all dependencies as a `requirements.txt`. Working dir can be provided by `dir` and the path for the virtual is customizable via `venv_dir`. `python_ensure_venv` will detect changes in the `requirements.txt` and only bootstrap the venv if needed.

```shell
source "python.sh"

echo "pytest-testinfra==7.0.0" > "requirements.txt"

python_ensure_venv

./venv/bin/python [...]
```

