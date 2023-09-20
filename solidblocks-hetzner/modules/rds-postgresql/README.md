# RDS PostgreSQL

See [documentation](https://pellepelster.github.io/solidblocks/hetzner/rds-postgresql/) for more details and usage examples.

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_hcloud"></a> [hcloud](#requirement\_hcloud) | >= 1.38.2 |
| <a name="requirement_http"></a> [http](#requirement\_http) | >= 3.3.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_hcloud"></a> [hcloud](#provider\_hcloud) | >= 1.38.2 |
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
| <a name="input_backup_s3_access_key"></a> [backup\_s3\_access\_key](#input\_backup\_s3\_access\_key) | AWS access key for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided. | `string` | `null` | no |
| <a name="input_backup_s3_bucket"></a> [backup\_s3\_bucket](#input\_backup\_s3\_bucket) | AWS bucket name for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided. | `string` | `null` | no |
| <a name="input_backup_s3_secret_key"></a> [backup\_s3\_secret\_key](#input\_backup\_s3\_secret\_key) | AWS secret key for S3 backups. To enable S3 backups `backup_s3_bucket` `backup_s3_access_key` and `backup_s3_secret_key` have to be provided. | `string` | `null` | no |
| <a name="input_backup_volume"></a> [backup\_volume](#input\_backup\_volume) | backup volume id | `string` | `0` | no |
| <a name="input_data_volume"></a> [data\_volume](#input\_data\_volume) | data volume id | `number` | n/a | yes |
| <a name="input_databases"></a> [databases](#input\_databases) | A list of databases to create when the instance is initialized, for example: `{ id : "database1", user : "user1", password : "password1" }`. Changing `user` and `password` is supported at any time, the provided config is translated into an config for the Solidblocks RDS PostgreSQL module (https://pellepelster.github.io/solidblocks/rds/index.html), please see https://pellepelster.github.io/solidblocks/rds/index.html#databases for more details of the database configuration. | `list(object({ id : string, user : string, password : string }))` | n/a | yes |
| <a name="input_extra_user_data"></a> [extra\_user\_data](#input\_extra\_user\_data) | deprecated, please use pre\_script/post\_script | `string` | `""` | no |
| <a name="input_labels"></a> [labels](#input\_labels) | A list of labels to be attached to the server instance. | `map(any)` | `{}` | no |
| <a name="input_location"></a> [location](#input\_location) | Hetzner location to use for provisioned resources | `string` | n/a | yes |
| <a name="input_mode"></a> [mode](#input\_mode) | startup mode for the database, can be empty to start the database or 'maintenance' to enable the maintenance mode of the underlying docker container to debug issues see also https://pellepelster.github.io/solidblocks//rds/#maintenance | `string` | `null` | no |
| <a name="input_name"></a> [name](#input\_name) | Unique name for the PostgreSQL instance | `string` | n/a | yes |
| <a name="input_network_id"></a> [network\_id](#input\_network\_id) | network the created sever should be attached to, network\_ip also needs to bet set in that case | `number` | `null` | no |
| <a name="input_network_ip"></a> [network\_ip](#input\_network\_ip) | ip address in the attached network | `string` | `null` | no |
| <a name="input_post_script"></a> [post\_script](#input\_post\_script) | shell script that will be executed after the server configuration is executed | `string` | `""` | no |
| <a name="input_postgres_extra_config"></a> [postgres\_extra\_config](#input\_postgres\_extra\_config) | Extra postgres configurations options for the postgresql.conf, see also https://pellepelster.github.io/solidblocks/rds/index.html#global -> DB\_POSTGRES\_EXTRA\_CONFIG | `string` | `null` | no |
| <a name="input_postgres_major_version"></a> [postgres\_major\_version](#input\_postgres\_major\_version) | PostgreSQL major version to use. Upgrading the version will trigger auto migration based on the underlying RDS PostgreSQL docker image, see also https://pellepelster.github.io/solidblocks/rds/index.html#versions. Please be aware that depending on the amount of data to migrate the migration may Terraforms timeouts, see https://pellepelster.github.io/solidblocks/hetzner/rds-postgresql/index.html#operations for debugging options. | `number` | `14` | no |
| <a name="input_pre_script"></a> [pre\_script](#input\_pre\_script) | shell script that will be executed before the server configuration is executed | `string` | `""` | no |
| <a name="input_public_net_ipv4_enabled"></a> [public\_net\_ipv4\_enabled](#input\_public\_net\_ipv4\_enabled) | enable/disable public ip addresses, see also https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server#public_net | `bool` | `true` | no |
| <a name="input_public_net_ipv6_enabled"></a> [public\_net\_ipv6\_enabled](#input\_public\_net\_ipv6\_enabled) | enable/disable public ip addresses, see also https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server#public_net | `bool` | `true` | no |
| <a name="input_server_type"></a> [server\_type](#input\_server\_type) | Hetzner cloud server type, supports x86 and ARM architectures | `string` | `"cx11"` | no |
| <a name="input_solidblocks_base_url"></a> [solidblocks\_base\_url](#input\_solidblocks\_base\_url) | override base url for testing purposes | `string` | `"https://github.com"` | no |
| <a name="input_solidblocks_cloud_init_version"></a> [solidblocks\_cloud\_init\_version](#input\_solidblocks\_cloud\_init\_version) | used for integration tests to inject test versions | `string` | `"v0.1.21"` | no |
| <a name="input_solidblocks_rds_version"></a> [solidblocks\_rds\_version](#input\_solidblocks\_rds\_version) | used for integration tests to inject test versions | `string` | `"v0.1.21"` | no |
| <a name="input_ssh_keys"></a> [ssh\_keys](#input\_ssh\_keys) | ssh keys to provision for instance access | `list(number)` | n/a | yes |
| <a name="input_ssl_dns_provider"></a> [ssl\_dns\_provider](#input\_ssl\_dns\_provider) | provider type to use for LetsEncrypt DNS challenge, see https://go-acme.github.io/lego/dns/ for available options | `string` | `""` | no |
| <a name="input_ssl_dns_provider_config"></a> [ssl\_dns\_provider\_config](#input\_ssl\_dns\_provider\_config) | environment configuration variable(s) to use for DNS provider selected via `ssl_dns_provider`, see documentation of selected provider for required configuration variables | `map(string)` | `{}` | no |
| <a name="input_ssl_domains"></a> [ssl\_domains](#input\_ssl\_domains) | domains to use for generated certificates | `list(string)` | `[]` | no |
| <a name="input_ssl_email"></a> [ssl\_email](#input\_ssl\_email) | email address to use for LetsEncrypt account creation | `string` | `""` | no |
| <a name="input_ssl_enable"></a> [ssl\_enable](#input\_ssl\_enable) | enable automatic ssl certificate creation using LetsEncrypt | `bool` | `false` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_ipv4_address"></a> [ipv4\_address](#output\_ipv4\_address) | IpV4 address of the created server if applicable |
| <a name="output_ipv4_address_private"></a> [ipv4\_address\_private](#output\_ipv4\_address\_private) | private IpV4 address of the created server if applicable |
| <a name="output_this_server_id"></a> [this\_server\_id](#output\_this\_server\_id) | Hetzner ID of the created server |
| <a name="output_user_data"></a> [user\_data](#output\_user\_data) | n/a |
<!-- END_TF_DOCS -->