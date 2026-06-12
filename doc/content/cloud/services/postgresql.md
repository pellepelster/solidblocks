+++
title = 'postgresql'
description = 'Single node PostgreSQL database instance with pgBackRest powered backup'
weight = 20
+++

The `postgresql` service deploys a single node PostgreSQL database instance with [pgBackRest](https://pgbackrest.org/) powered backup. PostgreSQL major versions 14 to 18 are supported, the default is 17.

Databases and their users are managed via the `databases` list. Strong credentials are generated for all users and automatically stored in the configured secret provider. Databases that are removed from the list will not be deleted automatically.

## Users

For every database a default user with full access is created, named after the database. Additional users can be declared in the `users` list of a database with permissions controlled via the `admin`, `read` and `write` flags

* `admin` grants full DDL privileges on the database
* `read` grants select access to all tables
* `write` grants insert, update and delete access to all tables and usage of all sequences

Permissions also cover tables created after the user was provisioned, and are re-applied on every apply, so removing a flag revokes the associated privileges. Users that are removed from the list are not deleted automatically. Each user can only be assigned to a single database, and user names must not collide with database names.

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
            admin: true
          - name: user2
            read: true
            write: true
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
