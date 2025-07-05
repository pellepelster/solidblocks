+++
title = "Role common"
description = "<no description>"
+++

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
| extension_pg_ivm_enabled | false | &lt;none&gt; | false  |
| extension_pgaudit_enabled | false | &lt;none&gt; | false  |
| extension_pglogical_enabled | false | &lt;none&gt; | false  |
| extension_pgvector_enabled | false | &lt;none&gt; | false  |
| extension_postgis_enabled | false | &lt;none&gt; | false  |
| postgres_data_dir | {{ storage_mount }}/{{ environment_name }}/{{ instance_name }} | &lt;none&gt; | false  |
| postgres_superuser_username | rds | &lt;none&gt; | false  |
| postgres_version | 17 | &lt;none&gt; | false  |
| storage_mount | /storage/data | &lt;none&gt; | false  |
