+++
title = "RDS PostgreSQL"
description = "RDS PostgreSQL Hetzner Terraform module"
overviewGroup = "hetzner"
faIcon = "fa-cloud"
+++

Based on the [RDS PostgreSQL]({{%relref "rds/_index.md" %}}) docker image this Terraform module provides a
ready to use PostgreSQL server that is backed up to a S3 compatible object store.

The module supports two backup methods either local storage (Hetzner Cloud Volume) or a S3 compatible object store. To
avoid data loss it is not possible to run the database without one of the backup methods enabled.

## Design Goals

Acknowledging that there are a lot of available solutions to run PostgreSQL in the cloud it is important to emphasize the design goals of this solution to be able to make an informed decision on if and how to use it

* First and foremost goal is to preserve the data stored in the PostgreSQL instance. This is ensured by an extensive set of [full integration tests](https://github.com/pellepelster/solidblocks/tree/main/solidblocks-rds-postgresql/test) replicating the intended usage scenarios
* Reduced complexity through single node operations. Running a PostgreSQL server with replication or in multi master comes with its own set of diverse failure scenarios and edge-cases. This solution aims at workloads that can easily handled by a single node and if in doubt favours [MTTR](https://de.wikipedia.org/wiki/Mean_Time_To_Recover) over [MTBF](https://de.wikipedia.org/wiki/Mean_Time_Between_Failures)
* Reduced complexity through less moving parts. Features like autoscaling, automatic recovery after zone outages, user interfaces, etc. like they are provided by the [Zalando PostgreSQL Operator](https://github.com/zalando/postgres-operator) provide a lot of value but also add multiple layers between the VM and the actual PostgreSQL instance. This solutions explicitly tries to avoid these features trying to keep the stack as simple as running a docker container on a VM.

## Usage

```terraform
{{% include "/snippets/hetzner-postgres-rds-s3-backup/instance/main.tf" %}}
```

For more usage examples see [Extended Usage]({{< ref "usage.md" >}})

## Operations

To access the created instance use one of SSH keys that was provided via `ssh_keys`. All linux system resources include the provided `name` to make them distinguishable.

### Show Logs

```shell
journalctl -u rds@rds-postgresql-${name}.service
```

### Start/Stop Database

```shell
systemctl [start|stop] rds@rds-postgresql-${name}.service 
```

### Maintenance Mode

In case of errors or the need for manual intervention a maintenance mode is available. Triggering the maintenance mode will set up the VM like it would be set up for database startup, but without actually starting the database. This allows to `exec` into the container to debug issues.

**main.tf**
```terraform
module "rds-postgresql" {
  source  = "pellepelster/solidblocks-rds-postgresql/hcloud"
  # [...]
  mode = "maintenance"
  # [...]
}
```

```shell
docker exec -ti rds-postgresql-${name}_postgresql /bin/bash
```


# Terraform Module

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_hcloud"></a> [hcloud](#requirement\_hcloud) | >=1.48.0 |
| <a name="requirement_http"></a> [http](#requirement\_http) | >= 3.3.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_hcloud"></a> [hcloud](#provider\_hcloud) | >=1.48.0 |
| <a name="provider_http"></a> [http](#provider\_http) | >= 3.3.0 |

## Resources

| Name | Type |
|------|------|
| [hcloud_server.rds](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server) | resource |
| [hcloud_volume_attachment.backup](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/volume_attachment) | resource |
| [hcloud_volume_attachment.data](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/volume_attachment) | resource |
| [hcloud_volume.backup](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/data-sources/volume) | data source |
| [hcloud_volume.data](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/data-sources/volume) | data source |
| [http_http.cloud_init_bootstrap_solidblocks](https://registry.terraform.io/providers/hashicorp/http/latest/docs/data-sources/http) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_backup_encryption_passphrase"></a> [backup\_encryption\_passphrase](#input\_backup\_encryption\_passphrase) | If set the backups will be encrypted using this passphrase | `string` | `null` | no |
| <a name="input_backup_full_calendar"></a> [backup\_full\_calendar](#input\_backup\_full\_calendar) | systemd timer spec for full backups | `string` | `"*-*-* 20:00:00"` | no |
| <a name="input_backup_incr_calendar"></a> [backup\_incr\_calendar](#input\_backup\_incr\_calendar) | systemd timer spec for incremental backups | `string` | `"*-*-* *:00:55"` | no |
| <a name="input_backup_local_retention_diff"></a> [backup\_local\_retention\_diff](#input\_backup\_local\_retention\_diff) | Local backup number of differential backups to retain. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-diff | `number` | `4` | no |
| <a name="input_backup_local_retention_full"></a> [backup\_local\_retention\_full](#input\_backup\_local\_retention\_full) | Local backups full backup retention count/time. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full | `number` | `7` | no |
| <a name="input_backup_local_retention_full_type"></a> [backup\_local\_retention\_full\_type](#input\_backup\_local\_retention\_full\_type) | Local backups retention policy type [count, time]. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full | `string` | `"count"` | no |
| <a name="input_backup_s3_access_key"></a> [backup\_s3\_access\_key](#input\_backup\_s3\_access\_key) | AWS access key for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided. | `string` | `null` | no |
| <a name="input_backup_s3_bucket"></a> [backup\_s3\_bucket](#input\_backup\_s3\_bucket) | AWS bucket name for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided. | `string` | `null` | no |
| <a name="input_backup_s3_host"></a> [backup\_s3\_host](#input\_backup\_s3\_host) | AWS host S3 backups. | `string` | `"s3.eu-central-1.amazonaws.com"` | no |
| <a name="input_backup_s3_region"></a> [backup\_s3\_region](#input\_backup\_s3\_region) | AWS region for S3 backups. | `string` | `"eu-central-1"` | no |
| <a name="input_backup_s3_retention_diff"></a> [backup\_s3\_retention\_diff](#input\_backup\_s3\_retention\_diff) | AWS S3 backup number of differential backups to retain. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-diff | `number` | `4` | no |
| <a name="input_backup_s3_retention_full"></a> [backup\_s3\_retention\_full](#input\_backup\_s3\_retention\_full) | AWS S3 backups full backup retention count/time. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full | `number` | `7` | no |
| <a name="input_backup_s3_retention_full_type"></a> [backup\_s3\_retention\_full\_type](#input\_backup\_s3\_retention\_full\_type) | AWS S3 backups retention policy type [count, time]. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full | `string` | `"count"` | no |
| <a name="input_backup_s3_secret_key"></a> [backup\_s3\_secret\_key](#input\_backup\_s3\_secret\_key) | AWS secret key for S3 backups. To enable S3 backups `backup_s3_bucket` `backup_s3_access_key` and `backup_s3_secret_key` have to be provided. | `string` | `null` | no |
| <a name="input_backup_volume"></a> [backup\_volume](#input\_backup\_volume) | backup volume id | `string` | `0` | no |
| <a name="input_data_volume"></a> [data\_volume](#input\_data\_volume) | data volume id | `number` | n/a | yes |
| <a name="input_databases"></a> [databases](#input\_databases) | A list of databases to create when the instance is initialized, for example: `{ id : "database1", user : "user1", password : "password1" }`. Changing `user` and `password` is supported at any time, the provided config is translated into an config for the Solidblocks RDS PostgreSQL module (https://pellepelster.github.io/solidblocks/rds/index.html), please see https://pellepelster.github.io/solidblocks/rds/index.html#databases for more details of the database configuration. | `list(object({ id : string, user : string, password : string }))` | n/a | yes |
| <a name="input_db_admin_password"></a> [db\_admin\_password](#input\_db\_admin\_password) | The database admin password. Username is always rds | `string` | `""` | no |
| <a name="input_db_backup_gcs_bucket"></a> [db\_backup\_gcs\_bucket](#input\_db\_backup\_gcs\_bucket) | Name of the Google Cloud storage bucket | `string` | `null` | no |
| <a name="input_db_backup_gcs_service_key"></a> [db\_backup\_gcs\_service\_key](#input\_db\_backup\_gcs\_service\_key) | content of the service key json file with appropriate permissions to write to the `db_backup_gcs_bucket` bucket. | `string` | `null` | no |
| <a name="input_environment_variables"></a> [environment\_variables](#input\_environment\_variables) | A list environment variables to pass to the PostgreSQL  docker container | `map(string)` | `{}` | no |
| <a name="input_extra_user_data"></a> [extra\_user\_data](#input\_extra\_user\_data) | deprecated, please use pre\_script/post\_script | `string` | `""` | no |
| <a name="input_firewall_disable"></a> [firewall\_disable](#input\_firewall\_disable) | disable automatic firewall configuration | `bool` | `false` | no |
| <a name="input_labels"></a> [labels](#input\_labels) | A list of labels to be attached to the server instance. | `map(any)` | `{}` | no |
| <a name="input_location"></a> [location](#input\_location) | Hetzner location to use for provisioned resources | `string` | n/a | yes |
| <a name="input_mode"></a> [mode](#input\_mode) | startup mode for the database, can be empty to start the database or 'maintenance' to enable the maintenance mode of the underlying docker container to debug issues see also https://pellepelster.github.io/solidblocks//rds/#maintenance | `string` | `null` | no |
| <a name="input_name"></a> [name](#input\_name) | Unique name for the PostgreSQL instance | `string` | n/a | yes |
| <a name="input_network_id"></a> [network\_id](#input\_network\_id) | network the created sever should be attached to, network\_ip also needs to bet set in that case | `number` | `null` | no |
| <a name="input_network_ip"></a> [network\_ip](#input\_network\_ip) | ip address in the attached network. when an ip address is provided the database server will automatically be bound to this ip and will not be exposed on any other network interfaces | `string` | `null` | no |
| <a name="input_post_script"></a> [post\_script](#input\_post\_script) | shell script that will be executed after the server configuration is executed | `string` | `""` | no |
| <a name="input_postgres_extra_config"></a> [postgres\_extra\_config](#input\_postgres\_extra\_config) | Extra postgres configurations options for the postgresql.conf, see also https://pellepelster.github.io/solidblocks/rds/index.html#global -> DB\_POSTGRES\_EXTRA\_CONFIG | `string` | `null` | no |
| <a name="input_postgres_major_version"></a> [postgres\_major\_version](#input\_postgres\_major\_version) | PostgreSQL major version to use. Upgrading the version will trigger auto migration based on the underlying RDS PostgreSQL docker image, see also https://pellepelster.github.io/solidblocks/rds/index.html#versions. Please be aware that depending on the amount of data to migrate the migration may Terraforms timeouts, see https://pellepelster.github.io/solidblocks/hetzner/rds-postgresql/index.html#operations for debugging options. | `number` | `14` | no |
| <a name="input_postgres_stop_timeout"></a> [postgres\_stop\_timeout](#input\_postgres\_stop\_timeout) | shutdown timeout for the postgres database in seconds, see also https://www.postgresql.org/docs/current/app-pg-ctl.html | `number` | `60` | no |
| <a name="input_pre_script"></a> [pre\_script](#input\_pre\_script) | shell script that will be executed before the server configuration is executed | `string` | `""` | no |
| <a name="input_public_net_ipv4_enabled"></a> [public\_net\_ipv4\_enabled](#input\_public\_net\_ipv4\_enabled) | enable/disable public ip addresses, see also https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server#public_net | `bool` | `true` | no |
| <a name="input_public_net_ipv6_enabled"></a> [public\_net\_ipv6\_enabled](#input\_public\_net\_ipv6\_enabled) | enable/disable public ip addresses, see also https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server#public_net | `bool` | `true` | no |
| <a name="input_restore_pitr"></a> [restore\_pitr](#input\_restore\_pitr) | Point in time to recover to, using the recovery type `time` as defined in https://pgbackrest.org/command.html#command-restore. Format should be `YYYY-MM-dd HH:mm:ssz` Please be aware that the server hosting the database might be in a different timezone, so always include the timezone when specifying PITR times `date +"%Y-%m-%d %H:%M:%S%z"` | `string` | `null` | no |
| <a name="input_server_type"></a> [server\_type](#input\_server\_type) | Hetzner cloud server type, supports x86 and ARM architectures | `string` | `"cx23"` | no |
| <a name="input_solidblocks_base_url"></a> [solidblocks\_base\_url](#input\_solidblocks\_base\_url) | override base url for testing purposes | `string` | `"https://github.com"` | no |
| <a name="input_solidblocks_cloud_init_version"></a> [solidblocks\_cloud\_init\_version](#input\_solidblocks\_cloud\_init\_version) | Solidblocks cloud-init version to use | `string` | `"v0.4.8-rc4"` | no |
| <a name="input_solidblocks_rds_version"></a> [solidblocks\_rds\_version](#input\_solidblocks\_rds\_version) | Solidblocks rds-postgresql version to use | `string` | `"v0.4.8-rc4"` | no |
| <a name="input_ssh_keys"></a> [ssh\_keys](#input\_ssh\_keys) | ssh keys to provision for instance access | `list(number)` | n/a | yes |
| <a name="input_ssl_acme_server"></a> [ssl\_acme\_server](#input\_ssl\_acme\_server) | The URL of the ACME Server to use. Defaults to Let's Encrypt production environment. | `string` | `"https://acme-v02.api.letsencrypt.org/directory"` | no |
| <a name="input_ssl_dns_provider"></a> [ssl\_dns\_provider](#input\_ssl\_dns\_provider) | provider type to use for LetsEncrypt DNS challenge, see https://go-acme.github.io/lego/dns/ for available options | `string` | `""` | no |
| <a name="input_ssl_dns_provider_config"></a> [ssl\_dns\_provider\_config](#input\_ssl\_dns\_provider\_config) | environment configuration variable(s) to use for DNS provider selected via `ssl_dns_provider`, see documentation of selected provider for required configuration variables | `map(string)` | `{}` | no |
| <a name="input_ssl_domains"></a> [ssl\_domains](#input\_ssl\_domains) | domains to use for generated certificates | `list(string)` | `[]` | no |
| <a name="input_ssl_email"></a> [ssl\_email](#input\_ssl\_email) | email address to use for LetsEncrypt account creation | `string` | `""` | no |
| <a name="input_ssl_enable"></a> [ssl\_enable](#input\_ssl\_enable) | enable automatic ssl certificate creation using LetsEncrypt | `bool` | `false` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_ipv4_address"></a> [ipv4\_address](#output\_ipv4\_address) | IPv4 address of the created server if applicable |
| <a name="output_ipv4_address_private"></a> [ipv4\_address\_private](#output\_ipv4\_address\_private) | private IPv4 address of the created server if applicable |
| <a name="output_ipv6_address"></a> [ipv6\_address](#output\_ipv6\_address) | IPv6 address of the created server if applicable |
| <a name="output_this_server_id"></a> [this\_server\_id](#output\_this\_server\_id) | Hetzner ID of the created server |
| <a name="output_user_data"></a> [user\_data](#output\_user\_data) | n/a |
<!-- END_TF_DOCS -->
