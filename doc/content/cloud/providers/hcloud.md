+++
title = 'hcloud'
description = 'Provides Hetzner Cloud based infrastructure resources'
weight = 10
+++

The `hcloud` provider provisions all infrastructure resources on [Hetzner Cloud](https://www.hetzner.com/cloud): virtual machines, volumes, networks, floating IPs and DNS entries if a `root_domain` is configured.

The location and instance type for the created virtual machines can be set via `default_location` (`fsn1`, `nbg1`, `hel1`, `ash`, `hil` or `sin`, default `fsn1`) and `default_instance_type` (default `cx23`), individual services can override both settings.

## Environment Variables

An API key with read/write access must be provided via the environment variable `HCLOUD_TOKEN`.

## Example

```yaml
name: cloud1

providers:
  - type: pass
  - type: ssh_key
  - type: hcloud
    default_location: nbg1
  - type: backup_local

services:
  #...
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
