+++
title = 'Services'
weight = 30
+++

Services are the workloads that Solidblocks Cloud deploys and manages. They are declared in the `services` list of the configuration file, where each entry selects a service `type` and a unique `name`

```yaml
services:
  - type: <s3|postgresql|docker|github_runner>
    name: service1
    #...
```

All services share a common set of configuration options like `environment_vars`, `use_floating_ip`, data and backup volume sizing and overrides for the virtual machine location and instance type. See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.

## Supported Services

### [`docker`]({{% relref "docker.md" %}})

Deploys a dockerized service and exposes its endpoints.

### [`postgresql`]({{% relref "postgresql.md" %}})

Single node PostgreSQL database instance with pgBackRest powered backup.

### [`s3`]({{% relref "s3.md" %}})

S3 compatible object storage service based on [GarageFS](https://garagehq.deuxfleurs.fr/).

### [`github_runner`]({{% relref "github-runner.md" %}})

Provisions self-hosted GitHub Actions runners based on the configured cloud provider.
