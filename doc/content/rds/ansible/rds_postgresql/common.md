+++
title = "Role common"
description = "Basic PostgreSQL setup"
+++

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
| data_dir | {{ storage_mount }}/{{ environment_name }}/{{ instance_name }} | &lt;none&gt; | false  |
| extension_pg_ivm_enabled | false | &lt;none&gt; | false  |
| extension_pgaudit_enabled | false | &lt;none&gt; | false  |
| extension_pglogical_enabled | false | &lt;none&gt; | false  |
| extension_pgvector_enabled | false | &lt;none&gt; | false  |
| extension_postgis_enabled | false | &lt;none&gt; | false  |
| extra_configuration | &lt;none&gt; | &lt;none&gt; | false  |
| postgres_version | 17 | &lt;none&gt; | false  |
| storage_mount | /storage/data | &lt;none&gt; | false  |
| superuser_username | rds | &lt;none&gt; | false  |
