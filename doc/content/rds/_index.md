+++
title = "RDS PostgreSQL"
description = "A PostgreSQL database with all batteries included backup solution powered by pgBackRest"
faIcon = "fa-brands fa-docker"
weight = 20
+++

A PostgreSQL database with all batteries included backup solution powered by pgBackRest available in two flavors

{{% children description="false" %}}

## Common Concepts

Although both implementations are quite different since one is implemented in Docker and uses mainly bash scripts where
the other one relies on Ansible for automation and orchestration, there are some similar concepts that apply to both
solutions.

### Packages

Both variants use the official [PostgreSQL](https://www.postgresql.org/download/linux/debian/) package repositories so
the database is always installed at `/usr/lib/postgresql/<postgres_version>/`

### Directory Layout and Naming

The storage path is built using the following pattern `<data_dir>/<instance_name>/<postgres_version>`.

| segment          | description                                                                           |
|------------------|---------------------------------------------------------------------------------------|
| data_dir         | base data directory, always mapped to non-ephemeral storage                           |
| instance_name    | unique name of the database instance to support multiple instances per node if needed |
| postgres_version | version of the PostgreSQL server                                                 |

During provisioning the follwing naming conventions are used when referencing the different parts of the storage path

```
   postgres_data_base_dir
            |
|---------------------- ---|
 <data_dir>/<instance_name>/<postgres_version>
|---------------------------------------------|
                     |
                postgres_data_dir

```

### Version Upgrades

If during startup/provisioning a `<data_dir>/<instance_name>/<postgres_version - 1>` directory is found and the
`<data_dir>/<instance_name>/<postgres_version>` directory is empty, an automatic migration process is started. In case
the migration is aborted oder cancelled, it can be re-triggered by removing the
`<data_dir>/<instance_name>/<postgres_version>` dir and restarting the provisioning again.

{{% notice info %}}
Currently only strictly sequential upgrades are allowed. E.g. to upgrade from PostgreSQL `15` to `17` an intermediate
upgrade to `16` needs to be performed first.
{{% /notice %}}

{{% notice style="note" %}}
Please keep in mind that the old data is kept and will not be deleted. This means that after an upgrade from `14`
to `15` `/storage/data/<db_instance_name>/14` is still present with the old data
and `/storage/data/<db_instance_name>/15` will contain the migrated version of the data
from `/storage/data/<db_instance_name>/14`
{{% /notice %}}
