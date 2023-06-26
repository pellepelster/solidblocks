---
title: RDS PostgreSQL
weight: 30
description: A containerized PostgreSQL database with an all batteries included backup solution powered by pgBackRest
---

A containerized [PostgreSQL](https://www.postgresql.org/) database with an all batteries included backup solution
powered
by [pgBackRest](https://pgbackrest.org/)

## Configuration

RDS PostgreSQL aims at being easy to use while keeping data a safe as possible. Based on the conventions of
the [official PostgreSQL docker image](https://hub.docker.com/_/postgres) it can be configured by tuning different
environment variables.

### Global

| configuration                    | type        | description                                                                                                                                                                     |
|----------------------------------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_INSTANCE_NAME`               | environment | unique name of this database instance                                                                                                                                           |
| `DB_ADMIN_PASSWORD`              | environment | Password for the db superuser, if not set a random password will be assigned. Username for the superuser is `rds`                                                               |
| `DB_POSTGRES_EXTRA_CONFIG`       | environment | Extra postgres configurations options for the `postgresql.conf`                                                                                                                 |
| /some/data/dir:/storage/data     | mount       | Container volume mount for the PostgreSQL data directory. The docker image uses a user with `uid` 10000, which needs to be reflected in the directory permissions               |
| /some/backup/dir:/storage/backup | mount       | Container volume mount for the pgBackRest backup repository directory. The docker image uses a group with `gid` 10000, which needs to be reflected in the directory permissions |

### Local Backup

Based on the functionality of [pgBackRest](https://pgbackrest.org/) two types of backup repositories are supported.
Local filesystem (`local`), or an S3 compatible object storage (`s3`). Those can be configured individually, but at
least one type has to be configured.

| configuration                         | type        | default | description                                                                                                                                                       |
|---------------------------------------|-------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_BACKUP_LOCAL`                     | environment | 0       | Flag to enable local filesystem as backup repository                                                                                                              |
| `DB_BACKUP_LOCAL_RETENTION_FULL_TYPE` | environment | count   | Retention type for full backups, see [retention type documentation](https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full-type) |
| `DB_BACKUP_LOCAL_RETENTION_FULL`      | environment | 7       | Retention for full backups, see [retention full documentation](https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full)           |
| `DB_BACKUP_LOCAL_RETENTION_DIFF`      | environment | 4       | Retention for diff backups, see [retention diff documentation](https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-diff)           |                                                                                                                                                                   |

### S3 Backup

S3 backup needs an S3 compatible backend. For non-AWS backends (like Minio) `DB_BACKUP_S3_URI_STYLE`
and `DB_BACKUP_S3_HOST`can be used to configure non-AWS servers.

| configuration                      | type        | default                       | description                                                                                                                                                                      |
|------------------------------------|-------------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_BACKUP_S3`                     | environment | 0                             | Flag to enable S3 object storage as backup repository                                                                                                                            |
| `DB_BACKUP_S3_HOST`                | environment | s3.eu-central-1.amazonaws.com | Hostname of the S3 object storage service                                                                                                                                        |
| `DB_BACKUP_S3_REGION`              | environment | eu-central-1                  | AWS region                                                                                                                                                                       |
| `DB_BACKUP_S3_BUCKET`              | environment | &lt;none&gt;                  | Bucket for the backup repository                                                                                                                                                 |
| `DB_BACKUP_S3_ACCESS_KEY`          | environment | &lt;none&gt;                  | Access key for the backup bucket                                                                                                                                                 |
| `DB_BACKUP_S3_SECRET_KEY`          | environment | &lt;none&gt;                  | Secret key for the backup bucket                                                                                                                                                 |
| `DB_BACKUP_S3_CA_PUBLIC_KEY`       | environment | &lt;none&gt;                  | Public key for the CA that issued the certificates for the `DB_BACKUP_S3_HOST`. Useful when a non SaaS solution like [minIO](https://min.io/) is used.                           |
| `DB_BACKUP_S3_URI_STYLE`           | environment | host                          | See [S3 uri style](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-uri-style) Useful when a non SaaS solution like [minIO](https://min.io/) is used. |
| `DB_BACKUP_S3_RETENTION_FULL_TYPE` | environment | count                         | Retention type for full backups, see [retention type documentation](https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full-type)                |
| `DB_BACKUP_S3_RETENTION_FULL`      | environment | 7                             | Retention for full backups, see [retention full documentation](https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full)                          |
| `DB_BACKUP_S3_RETENTION_DIFF`      | environment | 4                             | Retention for diff backups, see [retention diff documentation](https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-diff)                          |                                                                                                                                                                                  |

### Databases

Multiple databases can automatically be provisioned by providing configurations for multiple distinct
unique `${database_id}`s

| per database configuration   | type        | description                                                                      |
|------------------------------|-------------|----------------------------------------------------------------------------------|
| `DB_DATABASE_${database_id}` | environment | name of the database that will be crated when the PostgreSQL is initialized      | 
| `DB_USERNAME_${database_id}` | environment | name of the user who will be granted full access to `DB_DATABASE_${database_id}` |
| `DB_PASSWORD_${database_id}` | environment | password for the database user                                                   |

> `DB_USERNAME_${database_id}` and `DB_PASSWORD_${database_id}` can be changed at any time and will be re-provisioned on start to allow
> for easy password rotation or username change. Changing `DB_DATABASE_${database_id}` is currently not supported yet 

If any of those settings are missing or invalid, the container will complain with a log message and exit with an error
code:

```shell
docker run \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    pellepelster/solidblocks-rds-postgresql:__SOLIDBLOCKS_VERSION__ 

[solidblocks-rds-postgresql] either 'DB_BACKUP_S3' or 'DB_BACKUP_LOCAL' has to be activated
```

analogous if you try to start the container with the right configuration but with missing mounts it will result in
similar messages:

```shell
docker run \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_data:/storage/data" \
    pellepelster/solidblocks-rds-postgresql:__SOLIDBLOCKS_VERSION__

[solidblocks-rds-postgresql] local backup dir '/storage/backup' not mounted

docker run \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_backup:/storage/backup" \
    pellepelster/solidblocks-rds-postgresql:__SOLIDBLOCKS_VERSION__

[solidblocks-rds-postgresql] data dir '/storage/data' not mounted
```

Those safety checks are aimed at ensuring that there is always a working backup solution, and data is not accidentally
stored inside an ephemeral container.

On the first start RDS PostgreSQL will initialize the database according to the provided credentials, and create an
initial backup to validate the backup repositories are working as expected.

> The database inside the container runs with a non-root user with a `uid` and `gid` of 10000 so for the mounts the
> correct permissions need to be ensured

```shell
mkdir postgres_{data,backup} && sudo chown 10000:10000 postgres_{data,backup}

docker run \
    --name instance1 \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_backup:/storage/backup" \
    -v "$(pwd)/postgres_data:/storage/data" \
    pellepelster/solidblocks-rds-postgresql:__SOLIDBLOCKS_VERSION__

[solidblocks-rds-postgresql] data dir is empty
[solidblocks-rds-postgresql] initializing database instance
The files belonging to this database system will be owned by user "rds".
This user must also own the server process.

The database cluster will be initialized with locale "en_US.utf8".
The default text search configuration will be set to "english".

Data page checksums are disabled.

fixing permissions on existing directory /storage/data/instance1 ... ok
creating subdirectories ... ok
selecting dynamic shared memory implementation ... posix
selecting default max_connections ... 100
selecting default shared_buffers ... 128MB
selecting default time zone ... UTC
creating configuration files ... ok
running bootstrap script ... ok
performing post-bootstrap initialization ... ok
syncing data to disk ... ok

[...]

2022-10-02 18:11:07.671 P00   INFO: stanza-create command begin 2.36: --config=/rds/config/pgbackrest.conf --exec-id=66-4099fe2e --log-level-console=info --log-path=/rds/log --pg1-path=/storage/data/instance1 --pg1-socket-path=/rds/socket --repo2-path=/storage/backup --repo2-type=posix --stanza=instance1
2022-10-02 18:11:08.312 P00   INFO: stanza-create for stanza 'instance1' on repo2
2022-10-02 18:11:08.363 P00   INFO: stanza-create command end: completed successfully (694ms)
ensuring database 'database1' with user 'user1'
[solidblocks-rds-postgresql] creating database 'database1'
[solidblocks-rds-postgresql] creating user 'user1'
[solidblocks-rds-postgresql] granting all privileges for 'user1' on 'database1'
[solidblocks-rds-postgresql] setting permissions for 'user1'
[solidblocks-rds-postgresql] executing initial backup
2022-10-02 18:11:08.562 P00   INFO: backup command begin 2.36: --config=/rds/config/pgbackrest.conf --exec-id=88-ebcdde69 --log-level-console=info --log-path=/rds/log --pg1-path=/storage/data/instance1 --pg1-socket-path=/rds/socket --repo2-path=/storage/backup --repo2-retention-diff=7 --repo2-retention-full=7 --repo2-retention-full-type=count --repo2-type=posix --stanza=instance1 --type=full
2022-10-02 18:11:09.273 P00   INFO: execute non-exclusive pg_start_backup(): backup begins after the next regular checkpoint completes
2022-10-02 18:11:09.458 P00   INFO: archive-push command begin 2.36: [pg_wal/000000010000000000000001] --config=/rds/config/pgbackrest.conf --exec-id=90-a91e3518 --log-level-console=info --log-path=/rds/log --pg1-path=/storage/data/instance1 --repo2-path=/storage/backup --repo2-type=posix --stanza=instance1

[...]

2022-10-02 18:11:18.492 P00   INFO: check archive for segment(s) 000000010000000000000002:000000010000000000000002
2022-10-02 18:11:18.545 P00   INFO: pushed WAL file '000000010000000000000002' to the archive
2022-10-02 18:11:18.545 P00   INFO: archive-push command end: completed successfully (179ms)
2022-10-02 18:11:18.552 P00   INFO: archive-push command begin 2.36: [pg_wal/000000010000000000000002.00000028.backup] --config=/rds/config/pgbackrest.conf --exec-id=93-6e4c74bc --log-level-console=info --log-path=/rds/log --pg1-path=/storage/data/instance1 --repo2-path=/storage/backup --repo2-type=posix --stanza=instance1
2022-10-02 18:11:18.561 P00   INFO: pushed WAL file '000000010000000000000002.00000028.backup' to the archive
2022-10-02 18:11:18.561 P00   INFO: archive-push command end: completed successfully (10ms)
2022-10-02 18:11:18.617 P00   INFO: new backup label = 20221002-181109F
2022-10-02 18:11:18.721 P00   INFO: full backup size = 33.9MB, file total = 1244
2022-10-02 18:11:18.721 P00   INFO: backup command end: completed successfully (10160ms)
2022-10-02 18:11:18.722 P00   INFO: expire command begin 2.36: --config=/rds/config/pgbackrest.conf --exec-id=88-ebcdde69 --log-level-console=info

[...]

2022-10-02 18:11:18.882 GMT [1] LOG:  listening on IPv4 address "0.0.0.0", port 5432
2022-10-02 18:11:18.882 GMT [1] LOG:  listening on IPv6 address "::", port 5432
2022-10-02 18:11:18.889 GMT [1] LOG:  listening on Unix socket "/rds/socket/.s.PGSQL.5432"
2022-10-02 18:11:18.898 GMT [97] LOG:  database system was shut down at 2022-10-02 18:11:18 GMT
2022-10-02 18:11:18.903 GMT [1] LOG:  database system is ready to accept connections
```

## Backups

Backups can be triggered via

```shell
docker exec instance1 /rds/bin/backup-[full|incr|diff].sh
```

where `full`, `incr` and `diff` refer to the available backup types of
the [pgBackRest backup command](https://pgbackrest.org/command.html#command-backup)

Information about currently available backups can be retrieved with:

```shell
docker exec instance1 /rds/bin/backup-info.sh

stanza: instance1
    status: ok
    cipher: none

    db (current)
        wal archive min/max (14): 000000010000000000000001/000000010000000000000008

        full backup: 20221002-181109F
            timestamp start/stop: 2022-10-02 18:11:09 / 2022-10-02 18:11:18
            wal start/stop: 000000010000000000000002 / 000000010000000000000002
            database size: 33.9MB, database backup size: 33.9MB
            repo2: backup set size: 4.2MB, backup size: 4.2MB

        full backup: 20221002-182001F
            timestamp start/stop: 2022-10-02 18:20:01 / 2022-10-02 18:20:10
            wal start/stop: 000000010000000000000008 / 000000010000000000000008
            database size: 33.9MB, database backup size: 33.9MB
            repo2: backup set size: 4.2MB, backup size: 4.2MB
```

to restore a backup, just stop the container:

```shell
docker rm --force instance1
```

remove the data dir

```shell
sudo rm -rf postgres_data
mkdir postgres_data && sudo chown 10000:10000 postgres_data
```

and just restart the container again. It will automatically detect the empty data dir and restore the latest backup from
the backup repository.

```shell
docker run \
    --name instance1 \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_backup:/storage/backup" \
    -v "$(pwd)/postgres_data:/storage/data" \
    pellepelster/solidblocks-rds-postgresql:__SOLIDBLOCKS_VERSION__
``` 

## Extensions

The following PostgreSQL extensions are available by default

| name               | version | description                                                            |
|--------------------|---------|------------------------------------------------------------------------|
| adminpack          | 2.1     | administrative functions for PostgreSQL                                |
| amcheck            | 1.3     | functions for verifying relation integrity                             |
| autoinc            | 1.0     | functions for autoincrementing fields                                  |
| bloom              | 1.0     | bloom access method - signature file based index                       |
| btree_gin          | 1.3     | support for indexing common datatypes in GIN                           |
| btree_gist         | 1.6     | support for indexing common datatypes in GiST                          |
| citext             | 1.6     | data type for case-insensitive character strings                       |
| cube               | 1.5     | data type for multidimensional cubes                                   |
| dblink             | 1.2     | connect to other PostgreSQL databases from within a database           |
| dict_int           | 1.0     | text search dictionary template for integers                           |
| dict_xsyn          | 1.0     | text search dictionary template for extended synonym processing        |
| earthdistance      | 1.1     | calculate great-circle distances on the surface of the Earth           |
| file_fdw           | 1.0     | foreign-data wrapper for flat file access                              |
| fuzzystrmatch      | 1.1     | determine similarities and distance between strings                    |
| hstore             | 1.8     | data type for storing sets of (key, value) pairs                       |
| insert_username    | 1.0     | functions for tracking who changed a table                             |
| intagg             | 1.1     | integer aggregator and enumerator (obsolete)                           |
| intarray           | 1.5     | functions, operators, and index support for 1-D arrays of integers     |
| isn                | 1.2     | data types for international product numbering standards               |
| lo                 | 1.1     | Large Object maintenance                                               |
| ltree              | 1.2     | data type for hierarchical tree-like structures                        |
| moddatetime        | 1.0     | functions for tracking last modification time                          |
| old_snapshot       | 1.0     | utilities in support of old_snapshot_threshold                         |
| pageinspect        | 1.9     | inspect the contents of database pages at a low level                  |
| pg_buffercache     | 1.3     | examine the shared buffer cache                                        |
| pg_freespacemap    | 1.2     | examine the free space map (FSM)                                       |
| pg_prewarm         | 1.2     | prewarm relation data                                                  |
| pg_stat_statements | 1.9     | track planning and execution statistics of all SQL statements executed |
| pg_surgery         | 1.0     | extension to perform surgery on a damaged relation                     |
| pg_trgm            | 1.6     | text similarity measurement and index searching based on trigrams      |
| pg_visibility      | 1.2     | examine the visibility map (VM) and page-level visibility info         |
| pgcrypto           | 1.3     | cryptographic functions                                                |
| pgrowlocks         | 1.2     | show row-level locking information                                     |
| pgstattuple        | 1.5     | show tuple-level statistics                                            |
| plpgsql            | 1.0     | PL/pgSQL procedural language                                           |
| postgres_fdw       | 1.1     | foreign-data wrapper for remote PostgreSQL servers                     |
| refint             | 1.0     | functions for implementing referential integrity (obsolete)            |
| seg                | 1.4     | data type for representing line segments or floating-point intervals   |
| sslinfo            | 1.2     | information about SSL certificates                                     |
| tablefunc          | 1.0     | functions that manipulate whole tables, including crosstab             |
| tcn                | 1.0     | Triggered change notifications                                         |
| tsm_system_rows    | 1.0     | TABLESAMPLE method which accepts number of rows as a limit             |
| tsm_system_time    | 1.0     | TABLESAMPLE method which accepts time in milliseconds as a limit       |
| unaccent           | 1.1     | text search dictionary that removes accents                            |
| uuid-ossp          | 1.1     | generate universally unique identifiers (UUIDs)                        |
| xml2               | 1.1     | XPath querying and XSLT                                                |
