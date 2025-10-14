+++
title = "Standalone"
weight = 10
+++

## Overview

The full documentation of all configuration options is in the [standalone role documentation](rds_postgresql/standalone). For general configuration, have a look at the generic role [documentation](rds/ansible).

## Usage

### Add Solidblocks RDS collection to Ansible requirements

```yaml
---
collections:
  - name: https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/blcks-rds_postgresql-{{% env "SOLIDBLOCKS_VERSION" %}}.tar.gz
    type: url
```

### Apply role to database host
```yaml
---
- name: "database1"
  hosts: database1
  become: true
  roles:
    - role: blcks.rds_postgresql.standalone
      instance_name: database1
      environment_name: prod
      superuser_password: foobar
      backup_password: foobar
```

### Version upgrades

The database can be updated by setting the roles `<postgres_version>` variable to the desired major version, where all versions from the official PostgreSQL repository are available as described in the general [documentation](../#packages). 


For details of the update process please refer to this [part](../#version-upgrades) of the documentation, the following steps are recommended for a version upgrade:

* execute a full backup [backup](../runbooks/general/#trigger-full-backup)
* update `<postgres_version>` to desired target version
* execute Ansible playbook


## Provisioning Flow

```mermaid
flowchart TD
    provisioning_start@{shape: circle, label: "provisioning
start"} --> mount_data_device["`ensure **&lt;data_device&gt;** is mounted`"]
mount_data_device --> data_dir_empty{"`is **&lt;data_dir&gt;** 
    empty`"}
data_dir_empty -->|yes|restore_available{"`is backup 
    available`"}
restore_available -->|no|bootstrap_database
restore_available -->|yes|restore["`restore from backup`"]
restore --> wait_for_restore["`wait for recovery finish marker **/tmp/&lt;stanza_name&gt;-recovery-complete**`"]
wait_for_restore --> start_database_recovery["`start database for recovery`"]
start_database_recovery --> wait_for_recovery["`wait until **pg_is_in_recovery()** is **false**`"]
wait_for_recovery --> stop_database_recovery["`stop recovery database`"]
data_dir_empty -->|no| start_database["`start database`"]
bootstrap_database["`initialize **&lt;data_dir&gt;** with **&lt;superuser_*&gt;** credentials`"] --> start_database
stop_database_recovery --> start_database
start_database --> stanza_exists{"`backup
    stanza
    exists`"}
stanza_exists -->|yes| provisioning_finished@{shape: stadium, label: "provisioning finished"}
stanza_exists -->|no|stanza_create["`create backup stanza`"]
stanza_create --> provisioning_finished

```