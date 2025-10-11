+++
title = "Ansible"
description = "Ansible role to install a PostgreSQL database with all batteries included backup solution powered by pgBackRest"
overviewGroup = "rds"
faIcon = "fa-cloud"
weight = 1
+++

The Ansible role to install a [PostgreSQL](https://www.postgresql.org/) comes in two flavors:

* [standalone](./standalone) for single node installations
* [cluster](./cluster) featuring a primary/standby setup, supporting failover


## Common Concepts

Both variants are built similarly, and share a common set of configuration options and tooling that is described here, for detailed information about each variant, visit the documentation for [standalone](./standalone) and [cluster](./cluster).

To prevent service interruption, the standalone, as well as the cluster role, will only stop/restart the PostgreSQL
service if explicitly told to do so. Otherwise, it will just start the service after the initial provisioning and
otherwise not touch the running instance to avoid potential data loss or interference with manual processes.

### Directory Layout and Naming

To support running multiple instances per host, all names for services, folders, backups, etc. are prefixed with the concatenation of the variables `environment_name` and `instance_name` (`<environment_name>-<instance_name>`). 

The same applies for the data storage directory, that is built according to the naming scheme described [here](../##directory-layout-and-naming) where the `<instance_name>` is created from `<environment_name>/<instance_name>`.


### Backup

The database instance is backed up using pgBackRest. If started without any local data, restore will automatically be started if a remote backup is available.

Backups can be written to any S3 compatible bucket and are configured using the `backup_s3_*` variables. All backups are encrypted using the `backup_password`.

{{% notice warning %}}
Make sure you never loose the `backup_password` which is used to encrypt all backups. Without this password, restore will not work.
{{% /notice %}}

Per default full and incremental backups are enabled, the schedules can be tuned using the `backup_<full|incr>_schedule` variables.

### Superuser

The name as well as the password of the database superuser can be configured using the `superuser_<username|password>` variables.

### Extensions

Several PostgreSQL extensions are available for installation by switching on the `extension_*_enabled` variables.

### postgresql.conf

The roles manage all PostgreSQL configuration options needed to configure backup and clustering. Additional configuration options can be supplied using the variable `extra_configuration`.

```yaml
- name: "database1"
  hosts: database1
  roles:
    - role: blcks.rds_postgresql.standalone
      #[...]
      extra_configuration:
        debug_pretty_print: "on"
```
