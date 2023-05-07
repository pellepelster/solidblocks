---
title: Hetzner
weight: 50
---

A collection of infrastructure components for the [Hetzner Cloud](https://cloud.hetzner.de).

## RDS PostgreSQL

Based on the [RDS PostgreSQL]({{%relref "rds/_index.md" %}}) docker image this Terraform module provides a
ready to use PostgreSQL server that is backed up to a S3 compatible object store.

The module supports two backup methods either local storage (Hetzner cloud Volume) or a S3 compatible object store. To
avoid data loss it is not possible to run the database without one of the backup methods enabled.

| configuration             | default        | description                                                                                                                                                                                                                                                                                                                                                                                                      |
|---------------------------|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                      | &lt;none&gt;   | Unique identifier for the database instance. The id will be used for naming of all created cloud resources                                                                                                                                                                                                                                                                                                       |
| `location`                | &lt;none&gt;   | [Hetzner location](https://docs.hetzner.com/cloud/general/locations/) to use for cloud resource creation                                                                                                                                                                                                                                                                                                         |
| `ssh_keys`                | []             | one or more [Hetzner SSH](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/data-sources/ssh_key) key ids to use for provisioned VMs                                                                                                                                                                                                                                                       |
| `data_volume`             | &lt;none&gt;   | [Hetzner volume](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/data-sources/volume) id for the data disk                                                                                                                                                                                                                                                                               |
| `backup_volume`           | &lt;none&gt;   | [Hetzner volume](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/data-sources/volume) id for the backup disk, see [RDS PostgreSQL]({{%relref "rds/_index.md" %}}#local-backup) for more details on the configuration options. If provided local backup is used. Either a backup disk or `db_backup_s3_*` need to be provided, the instance will not start without a valid backup method. |
| `db_backup_s3_bucket`     | &lt;none&gt;   | To enable backup to S3, bucket name, access key and secret key need to be provided, see [RDS PostgreSQL]({{%relref "rds/_index.md" %}}#s3-backup) for more details on the configuration options. If provided S3 backup is used. Eiter a backup disk or `db_backup_s3_*` need to be provided, the instance will not start without a valid backup method.                                                          |
| `db_backup_s3_access_key` | &lt;none&gt;   | see `db_backup_s3_bucket`                                                                                                                                                                                                                                                                                                                                                                                        |
| `db_backup_s3_secret_key` | &lt;none&gt;   | see `db_backup_s3_bucket`                                                                                                                                                                                                                                                                                                                                                                                        |
| `backup_full_calendar`    | *-*-* 20:00:00 | systemd timer spec for full backups, see also [systemd.time](https://man.archlinux.org/man/systemd.time.7)                                                                                                                                                                                                                                                                                                       |
| `backup_incr_calendar`    | *-*-* *:00:55  | systemd timer spec for incremental backups, see also [systemd.time](https://man.archlinux.org/man/systemd.time.7)                                                                                                                                                                                                                                                                                                |
| `server_type`             | cx11           | [Hetzner server type](https://docs.hetzner.com/cloud/servers/overview)                                                                                                                                                                                                                                                                                                                                           |
| `databases`               | &lt;none&gt;   | A list of databases to create when the instance is initialized, for example: ```{ id : "database1", user : "user1", password : "password1" }```                                                                                                                                                                                                                                                                  |

### S3 Backed Backup

Below a minimal example of a PostgreSQL database using S3 as backup storage backend.

**main.tf**

```shell
{{% include "/snippets/hetzner-postgres-rds-s3-backup/main.tf" %}}
```

**variables.tf**

```shell
{{% include "/snippets/hetzner-postgres-rds-s3-backup/variables.tf" %}}
```

### Local attached storage Backup

Below a minimal example of a PostgreSQL database using a local volume backup storage backend.

**main.tf**

```shell
{{% include "/snippets/hetzner-postgres-rds-local-backup/main.tf" %}}
```

**variables.tf**

```shell
{{% include "/snippets/hetzner-postgres-rds-local-backup/variables.tf" %}}

```
