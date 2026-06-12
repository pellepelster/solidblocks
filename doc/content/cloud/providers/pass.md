+++
title = 'pass'
description = 'Stores secrets in the pass password manager'
weight = 30
+++

The `pass` provider stores all generated secrets in the [pass](https://www.passwordstore.org/) password manager, the `pass` command line tool must be available locally.

The storage path for the password store can be set via `password_store_dir`, if not set the default or the setting from the `PASSWORD_STORE_DIR` environment variable will be used. To ensure that the store is set up correctly a temporary secret is created and deleted during the configuration validation phase, the validation can be skipped by setting the environment variable `BLCKS_PASS_PROVIDER_SKIP_VALIDATION`.

## Example

```yaml
name: cloud1

providers:
  - type: pass
    password_store_dir: ~/.password-store-cloud1
  - type: ssh_key
  - type: hcloud
  - type: backup_local

services:
  #...
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
