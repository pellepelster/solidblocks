---
title: Usage
weight: 99
---

On startup RDS PostgreSQL performs extensive checks to ensure all required settings are configured in a way that no data can be lost. If something is missing the container will complain with a log message and exit with an error code:

```shell
docker run \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    ghcr.io/pellepelster//solidblocks-rds-postgresql:14-v{{% env "SOLIDBLOCKS_VERSION" %}} 

[solidblocks-rds-postgresql] either 'DB_BACKUP_S3' or 'DB_BACKUP_LOCAL' has to be activated
```

analogous if you try to start the container with the right configuration but with missing mounts it will result in similar messages:

```shell
docker run \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_data:/storage/data" \
    ghcr.io/pellepelster/solidblocks-rds-postgresql:14-v{{% env "SOLIDBLOCKS_VERSION" %}}

[solidblocks-rds-postgresql] local backup dir '/storage/backup' not mounted

docker run \
    -e DB_INSTANCE_NAME=instance1 \
    -e DB_DATABASE_db1=database1 \
    -e DB_USERNAME_db1=user1 \
    -e DB_PASSWORD_db1=password1 \
    -e DB_BACKUP_LOCAL=1 \
    -v "$(pwd)/postgres_backup:/storage/backup" \
    ghcr.io/pellepelster/solidblocks-rds-postgresql:14-v{{% env "SOLIDBLOCKS_VERSION" %}}

[solidblocks-rds-postgresql] data dir '/storage/data' not mounted
```

Those safety checks are aimed at ensuring that there is always a working backup solution, and data is not accidentally stored inside an ephemeral container.

On the first start RDS PostgreSQL will initialize the database according to the provided credentials, and create an initial backup to validate the backup repositories are working as expected.

{{% notice tip %}}
The database inside the container runs with a non-root user with a `uid` and `gid` of 10000 so for the mounts the
correct permissions need to be ensured.
{{% /notice %}}

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
    ghcr.io/pellepelster/solidblocks-rds-postgresql:v14-v{{% env "SOLIDBLOCKS_VERSION" %}}

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

Backups can be triggered via calling one if the following commands

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

and just restart the container again. It will automatically detect the empty data dir and restore the latest backup from the backup repository.

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
    ghcr.io/pellepelster/solidblocks-rds-postgresql:14-v{{% env "SOLIDBLOCKS_VERSION" %}}
``` 
{{% notice warning %}}
Backups need to be triggered from outside the of docker container, because Solidblocks adheres to the convention that each docker container should run a single, isolated process, and thus does not contain a cron scheduler.
{{% /notice %}}

## Operations

The container includes a set of scripts for various maintenance operations and day to day tasks that are available on the `${PATH}`

* `backup-full.sh` trigger a full backup
* `backup-diff.sh` trigger a differential backup
* `backup-incr.sh` trigger an incremental backup
* `backup-info.sh` show information about backups
* `rds_provisioning_completed.sh` exits with 0 when provisioning and/or restore has completed


### Access the database locally

In case you need to interact with the database directly, you can always `exec` into the docker container and use `psql` directly

```shell
docker exec -ti <container> bash
psql -h /rds/socket -d postgres
```

### Manual password reset 

```shell
docker exec -ti <container> bash
psql -h /rds/socket -d postgres
ALTER USER rds WITH PASSWORD '<new admin password>';
```

### Maintenance

In case of errors or the need for manual intervention a maintenance mode is available. Triggering the maintenance mode will set up the container like it would be set up for database startup, but without actually starting the database. This allows to `exec` into the container to debug issues.

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
    ghcr.io/pellepelster/solidblocks-rds-postgresql:14-v{{% env "SOLIDBLOCKS_VERSION" %}} \
    maintenance
```
