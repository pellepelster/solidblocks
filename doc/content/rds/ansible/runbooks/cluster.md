+++
title = "Cluster operations"
description = "Failover prodecures for PostgreSQL clusters"
+++


## Show replication status

On the primary node run to show some replication statistics

```postgresql
SELECT pid, client_addr, state, sync_state, sent_lsn, write_lsn, flush_lsn, replay_lsn, write_lag, flush_lag, replay_lag FROM pg_stat_replication;
```

The most interesting metric here is the `replay_lag` showing the time elapsed between flushing recent WAL locally and receiving notification that the secondary node has written, flushed and applied it.

## Cluster node failover

### Verify primary and secondary nodes

Before starting failover from a primary node to a secondary, verify the status of the affected nodes

```postgresql
SELECT * FROM pg_is_in_recovery();
```

returns `t` on a secondary node, because it is recovering and following a primary node. The primary node should return `f` here.


### Shutdown primary node

Now on the primary node stop the instance with

```shell
systemctl stop <environment_name>-<instance_name>
```

and wait until the shutdown finishes.

### Remove recovery configuration

Secondary nodes are bootstrapped from a pgBackrest backup, and then attached to the primary node by providing a `primary_conninfo` configuration pointing to the primary node. This configuration is then written to `/storage/data/<environment_name>/<instance_name>/postgresql.auto.conf` which gets autoloaded by PostgreSQL on startup. 

To transition a secondary to primary, remove the recovery config on the secondary node and restart the instance

```shell
rm /storage/data/<environment_name>/<instance_name>/postgresql.auto.conf
systemctl restart <environment_name>-<instance_name>
```

### Promote secondary to primary

To finish the transition now on the secondary promote the database instance to primary

```postgresql
SELECT pg_promote();
```

and verify that the propagation was successful and the node now is primary with

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

### Restore new secondary node

To create a new secondary node that follows the new primary execute the ansible role again, with the new primary provided as `primary_node`. 

If you plan to re-use the old primary node, make sure to move the old data aside first to trigger the restore

```shell
mv /storage/data/<environment_name>/<instance_name> /storage/data/<environment_name>/<instance_name>.old
```


