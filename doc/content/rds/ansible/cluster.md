+++
title = "Cluster"
weight = 20
+++

## Overview

The full documentation of all configuration options is in the [cluster role documentation](rds_postgresql/cluster). For
general configuration, have a look at the generic role [documentation](rds/ansible).

## Usage

### Add Solidblocks RDS collection to Ansible requirements

```yaml
---
collections:
  - name: https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/blcks-rds_postgresql-{{% env "SOLIDBLOCKS_VERSION_RAW" %}}.tar.gz
    type: url
```

### Apply role to database host group

The role should be applied to a group of hosts, where the primary node will be the node configured by `primary_node`.

```yaml
---
- name: "database2"
  hosts: database2
  become: true
  roles:
    - role: blcks.rds_postgresql.cluster
      instance_name: database2
      environment_name: prod
      superuser_password: foobar
      backup_password: foobar
      replicator_password: foobar
      private_subnet: "10.0.1.0/24"
```

## Configuration

The cluster setup has some additional mandatory configuration options

| variable              | description                                                                |
|-----------------------|----------------------------------------------------------------------------|
| `primary_node`        | Ansible hostname of the primary node                                       |
| `private_subnet`      | IP subnet that is shared by all database nodes                             |
| `replicator_password` | credentials used to authenticate the read replicas against to primary node |

## Provisioning Flow

```mermaid
flowchart TD
    provisioning_start@{shape: circle, label: "provisioning
start"} --> mount_data_device["`ensure **&lt;data_device&gt;** is mounted`"]
mount_data_device --> is_primary{"`is primary node`"}
is_primary -->|yes|data_dir_empty
data_dir_empty{"`is **&lt;data_dir&gt;** 
    empty`"} -->|yes|restore_available{"`is backup 
    available`"}
restore_available -->|no|bootstrap_database
restore_available -->|yes|restore["`restore from backup`"]
restore --> wait_for_restore["`wait for recovery finish marker **/tmp/&lt;stanza_name&gt;-recovery-complete**`"]
wait_for_restore --> start_database_recovery["`start database for recovery`"]
start_database_recovery --> wait_for_recovery["`wait until **pg_is_in_recovery()** is **false**`"]
wait_for_recovery --> stop_database_recovery["`stop recovery database`"]
data_dir_empty -->|no|start_database["`start database`"]
bootstrap_database["`initialize **&lt;data_dir&gt;** with **&lt;superuser_*&gt;** credentials`"] --> start_database
stop_database_recovery --> start_database
start_database --> stanza_exists{"`backup
    stanza
    exists`"}
stanza_exists -->|yes|create_replication_user
stanza_exists -->|no|stanza_create["`create backup stanza`"]
stanza_create --> create_replication_user["`creation replication user`"]
create_replication_user --> provisioning_finished@{shape: stadium, label: "provisioning finished"}

is_primary -->|no| wait_provisioning_finished["`wait for provisioning 
to finish on primary`"]

wait_provisioning_finished --> restore_from_backup["`restore node from 
primary backup`"]

restore_from_backup --> config_secondary["`configure secondary with **primary_conninfo**
pointing to primary node`"]

config_secondary --> pg_pass["`add replication credentials to **.pgpass**`"]

pg_pass --> start_secondary["`start database`"]

start_secondary --> provisioning_finished
```