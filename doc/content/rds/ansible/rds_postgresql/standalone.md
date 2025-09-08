+++
title = "Role standalone"
description = "Provisions a standalone PostgreSQL server"
+++

Provisions a standalone PostgreSQL server

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
| backup_full_schedule | *-*-* 02:00:02 | Systemd calendar schedule for full backups | false  |
| backup_incr_schedule | *-*-* 0/3:00:02 | Systemd calendar schedule for incremental backups | false  |
| backup_password | &lt;none&gt; | S3 backup [encryption password](https://pgbackrest.org/configuration.html#section-repository/option-repo-cipher-pass) | true  |
| backup_s3_bucket | &lt;none&gt; | S3 backup bucket [name](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-bucket) | true  |
| backup_s3_endpoint | &lt;none&gt; | S3 backup bucket [endpoint](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-endpoint) | true  |
| backup_s3_key | &lt;none&gt; | S3 backup bucket [access key](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-key) | true  |
| backup_s3_key_secret | &lt;none&gt; | S3 backup bucket [secret access key](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-key-secret) | true  |
| backup_s3_region | &lt;none&gt; | S3 backup bucket [region](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-region) | true  |
| backup_s3_uri_style | host | S3 backup bucket [uri style](https://pgbackrest.org/configuration.html#section-repository/option-repo-s3-uri-style) | false  |
| environment_name | &lt;none&gt; | environment name | true  |
| extension_pg_ivm_enabled | false | enable postgis postgres extension | false  |
| extension_pgaudit_enabled | false | enable pgaudit postgres extension | false  |
| extension_pglogical_enabled | false | enable pglogical postgres extension | false  |
| extension_pgvector_enabled | false | enable postgis postgres extension | false  |
| extension_postgis_enabled | false | enable postgis postgres extension | false  |
| extra_configuration | &lt;none&gt; | additional database configuration options as key/value pairs | false  |
| instance_name | &lt;none&gt; | database instance name | true  |
| postgres_version | &lt;none&gt; | postgres version to install | false  |
| primary_node | {{ ansible_hostname }} | &lt;none&gt; | false  |
| service_name | {{ environment_name }}-{{ instance_name }} | &lt;none&gt; | false  |
| stanza_name | {{ service_name }} | &lt;none&gt; | false  |
| superuser_password | &lt;none&gt; | database superuser password | true  |
| superuser_username | &lt;none&gt; | database superuser name | false  |