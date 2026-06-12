+++
title = 'ssh_key'
description = 'Loads local file based SSH keys'
weight = 20
+++

The `ssh_key` provider loads local file based SSH keys, which are used to manage the provisioned virtual machines over SSH. It supports passwordless PEM as well as OpenSSH encoded private keys, exactly one `ssh_key` provider is required in every configuration.

The key can be set explicitly via `private_key`. If not set, a key named `<cloud-name>.key` next to the configuration file is tried first, then the default SSH key paths (`~/.ssh/id_rsa`, `~/.ssh/id_ecdsa`, `~/.ssh/id_ecdsa_sk`, `~/.ssh/id_ed25519`, `~/.ssh/id_ed25519_sk`). The private key must not be password protected and its file permissions must be restricted to the owner.

## Example

```yaml
name: cloud1

providers:
  - type: pass
  - type: ssh_key
    private_key: cloud1.key
  - type: hcloud
  - type: backup_local

services:
  #...
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
