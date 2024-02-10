---
title: CI
weight: 60
description: Utilities for various CI/CD systems
---

Utilities for various CI/CD systems

## Functions

    ### `ci_detected()` {#ci_detected}

Will return `true` if it detects a CI system by looking at the currently set environment variables and `false` otherwise.
Supports all CI systems supported by [ci-info](https://github.com/watson/ci-info/tree/master).

```shell
source "ci.sh"

if [[ $(ci_detected) == "true" ]]; then
  echo "we are running in CI"
else
  echo "we are running somewhere else"
fi
```

