+++
title = 'postgresql'
description = 'Single node PostgreSQL database instance with pgBackRest powered backup'
weight = 20
+++

The `postgresql` service deploys a single node PostgreSQL database instance with [pgBackRest](https://pgbackrest.org/) powered backup. PostgreSQL major versions 14 to 18 are supported, the default is 17.

Databases and their users are managed via the `databases` list. Strong credentials are generated for all users and automatically stored in the configured secret provider. Databases that are removed from the list will not be deleted automatically.

## Required Providers

A cloud provider (`hcloud`), a backup provider (`backup_local` or `backup_aws_s3`) and a secret provider (`pass` or `protonpass`).

## Example

```yaml
name: cloud1

providers:
  - type: pass
  - type: ssh_key
  - type: hcloud
  - type: backup_local

services:
  - type: postgresql
    name: database1
    majorVersion: 17
    databases:
      - name: hello-world
        users:
          - name: user1
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
