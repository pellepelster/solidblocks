---
title: Usage
weight: 50
description: RDS PostgreSQL Terraform module
---

To prevent accidental data deletion it has proven useful to put the storage devices for the database (data and backup) into a separate terraform module. This also makes it easier to re-provision the instance without risking deleting valuable data.

The examples below embrace this pattern, so each example has two different modules:

* `/storage` contains all `hcloud_volume` resources for the databases
* `instance` contains the database instance itself, the storage id is retrieved using the `hcloud_volume` data lookup

## S3 Backed Backup

Below a minimal example of a PostgreSQL database using S3 as backup storage backend. The full example can be downloaded from the [latest release](https://github.com/pellepelster/solidblocks/releases/latest/).

**instance/main.tf**

```terraform
{{% include "/snippets/hetzner-postgres-rds-s3-backup/instance/main.tf" %}}
```

## Local attached storage Backup

Below a minimal example of a PostgreSQL database using a local volume backup storage backend. The full example can be downloaded from the [latest release](https://github.com/pellepelster/solidblocks/releases/latest/).

**instance/main.tf**

```terraform
{{% include "/snippets/hetzner-postgres-rds-local-backup/instance/main.tf" %}}
```


## Private Networking Only
This example places the database instance in a private network so that it is not reachable from the internet.

```terraform
{{% include "/snippets/hetzner-postgres-rds-private-network/instance/main.tf" %}}
```

## Hetzner Object Storage
This examples show how to use Hetzner Object storage as a provider for the S3 bucket. The full example can be downloaded from the [latest release](https://github.com/pellepelster/solidblocks/releases/latest/).

```terraform
{{% include "/snippets/hetzner-postgres-rds-hetzner-s3-backup/instance/main.tf" %}}
```
