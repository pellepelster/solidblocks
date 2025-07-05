+++
title = "Role cluster"
description = "setup PostgreSQL database"
+++

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
| backup_password | &lt;none&gt; | pgbackrest encryption password | true  |
| backup_s3_bucket | &lt;none&gt; | TODO | true  |
| backup_s3_endpoint | &lt;none&gt; | TODO | true  |
| backup_s3_key | &lt;none&gt; | TODO | true  |
| backup_s3_key_secret | &lt;none&gt; | TODO | true  |
| backup_s3_region | &lt;none&gt; | TODO | true  |
| backup_s3_uri_style | host | TODO | false  |
| environment_name | &lt;none&gt; | environment name | true  |
| extension_pg_ivm_enabled | &lt;none&gt; | enable postgis postgres extension | false  |
| extension_pgaudit_enabled | &lt;none&gt; | enable pgaudit postgres extension | false  |
| extension_pglogical_enabled | &lt;none&gt; | enable pglogical postgres extension | false  |
| extension_pgvector_enabled | &lt;none&gt; | enable postgis postgres extension | false  |
| extension_postgis_enabled | &lt;none&gt; | enable postgis postgres extension | false  |
| instance_name | &lt;none&gt; | database instance name | true  |
| pgbackrest_bucket_name | {{ service_name }} | &lt;none&gt; | false  |
| pgbackrest_stanza_name | {{ service_name }} | &lt;none&gt; | false  |
| postgres_replicator_password | TODO | &lt;none&gt; | false  |
| postgres_replicator_username | TODO | &lt;none&gt; | false  |
| postgres_superuser_password | &lt;none&gt; | database superuser password | true  |
| postgres_superuser_username | &lt;none&gt; | database superuser name | false  |
| postgres_version | &lt;none&gt; | postgres version to install | false  |
| primary_node | &lt;none&gt; | name of the primary database node | true  |
| service_name | {{ environment_name }}-{{ instance_name }} | &lt;none&gt; | false  |
| todo | TODO | &lt;none&gt; | false  |
| todo_cidr | 192.168.0.0/24 | &lt;none&gt; | false  |
