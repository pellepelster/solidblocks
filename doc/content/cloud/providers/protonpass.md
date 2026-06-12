+++
title = 'protonpass'
description = 'Stores secrets in a Proton Pass vault'
weight = 40
+++

The `protonpass` provider stores all generated secrets in a [Proton Pass](https://proton.me/pass) vault using the `pass-cli` command line tool, which must be available and authenticated locally.

The vault can be selected via `vault_name`, if not set the cloud name will be used. To ensure that the vault is set up correctly a temporary secret is created and deleted during the configuration validation phase, the validation can be skipped by setting the environment variable `BLCKS_PROTONPASS_PROVIDER_SKIP_VALIDATION`.

## Example

```yaml
name: cloud1

providers:
  - type: protonpass
    vault_name: cloud1-secrets
  - type: ssh_key
  - type: hcloud
  - type: backup_local

services:
  #...
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
