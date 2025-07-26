+++
title = "Cluster operations"
description = "Failover prodecures for PostgreSQL clusters"
+++


## Show replication status

On the primary node run to show some replication statistics

```postgresql
SELECT pid, client_addr, state, sync_state, sent_lsn, write_lsn, flush_lsn, replay_lsn, write_lag, flush_lag, replay_lag FROM pg_stat_replication;
```

The most interesting metric here is the `replay_lag` showing the time elapsed between flushing recent WAL locally and receiving notification that the standby node has written, flushed and applied it.

## Cluster node failover

The following steps will guide you through the process of promoting a standby node to primary

### Verify primary and standby nodes

If the failover is planned and the primary is still alive, start by verifying the status of all nodes affected by the failover to avoid any mistakes

```postgresql
SELECT * FROM pg_is_in_recovery();
```

returns `t` on a standby node, because it is "recovering" and following a primary node by streaming its WAL. The primary node should return `f` here.


### Shutdown primary node

Again if the old primary is sill alive, stop the PostgreSQL instance with

```shell
systemctl stop <environment_name>-<instance_name>
```

and wait until the shutdown finishes.

### Remove recovery configuration

Standby nodes are bootstrapped from a pgBackrest backup, and then attached to the primary node by providing a `primary_conninfo` configuration pointing to the primary node. This configuration is then written to `/storage/data/<environment_name>/<instance_name>/postgresql.auto.conf` which gets autoloaded by PostgreSQL on startup. As long as that config is present, the node will assume its a standby node, and try to connect to the primary.

To transition a standby node to primary, remove the recovery config on the standby node and restart the database instance

```shell
rm /storage/data/<environment_name>/<instance_name>/postgresql.auto.conf
systemctl restart <environment_name>-<instance_name>
```

### Promote standby to primary

To finish the transition now on the standby promote the database instance to primary

```postgresql
SELECT pg_promote();
```

and verify that the promotion was successful and the node now is primary with

```postgresql
SELECT * FROM pg_is_in_recovery();
```

which should return `f` now.

### Trigger a backup

When the failover process is done, PostgreSQL internally creates a new [timeline](https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-TIMELINES) to be able to track changes from that point onward. This ensures that previous Write-Ahead Log (WAL) data isn’t overwritten. Because the latest backup was created on the old node with the previous timeline, we need to trigger a new backup, so the backup repository knows the latest timeline 

On the new primary trigger a backup

```shell
<environment_name>-<instance_name>-backup-incr.sh
```

If you are in doubt which node has the latest timeline, you can query the current using

```postgresql
SELECT timeline_id FROM pg_control_checkpoint();
```

### Restore new standby node

To create a new standby node that follows the new primary execute the ansible role again, with the new primary provided as `primary_node`. 

If you plan to re-use the old primary node, make sure to move the old data aside first to trigger the restore

```shell
mv /storage/data/<environment_name>/<instance_name> /storage/data/<environment_name>/<instance_name>.old
```
