+++
title = "General operations"
description = "Basic commands for day-to-day operations"
+++

Basic commands for day-to-day operations

## Start and stop database instance

```shell
systemctl <stop|start|restart> <environment_name>-<instance_name>
```

## Show database instance log

```shell
journalctl --follow --unit <environment_name>-<instance_name>
```

## Show available backups

**using the wrapper**
```shell
<environment_name>-<instance_name>-pgbackrest info
```

**using pgbackrest direct**
```shell
pgbackrest --stanza=<environment_name>-<instance_name> info
```

## Start psql shell

**using the wrapper**
```shell
<environment_name>-<instance_name>-psql postgres
```
**using psql direct**
```shell
sudo -u postgres psql --user <superuser_name> postgres
```
