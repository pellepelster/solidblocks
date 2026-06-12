+++
title = 'docker'
description = 'Deploys a dockerized service and exposes its endpoints'
weight = 10
+++

The `docker` service deploys a dockerized service on a dedicated virtual machine and exposes its endpoints. Endpoints of the type `http` are automatically terminated with TLS if a `root_domain` is set for the cloud configuration.

Other services can be referenced via `links`, linked services automatically expose environment variables to the linking service, e.g. database credentials for a linked `postgresql` service. To see which variables are available run the `info` command.

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
    databases:
      - name: hello-world

  - type: docker
    name: service1
    image: ghcr.io/pellepelster/solidblocks-hello-world:latest
    endpoints:
      - container_port: 8080
    links:
      - database1
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
