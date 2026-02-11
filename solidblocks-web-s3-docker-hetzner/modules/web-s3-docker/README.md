# Web S3 Docker Hetzner

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_hcloud"></a> [hcloud](#requirement\_hcloud) | >=1.56.0 |
| <a name="requirement_http"></a> [http](#requirement\_http) | >= 3.3.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_hcloud"></a> [hcloud](#provider\_hcloud) | >=1.56.0 |
| <a name="provider_random"></a> [random](#provider\_random) | n/a |

## Resources

| Name | Type |
|------|------|
| [hcloud_primary_ip.ipv4](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/primary_ip) | resource |
| [hcloud_primary_ip.ipv6](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/primary_ip) | resource |
| [hcloud_server.server](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server) | resource |
| [hcloud_volume.data](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/volume) | resource |
| [hcloud_volume_attachment.data](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/volume_attachment) | resource |
| [hcloud_zone_rrset.root_domain_catchall_ipv4](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/zone_rrset) | resource |
| [hcloud_zone_rrset.root_domain_ipv4](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/zone_rrset) | resource |
| [hcloud_zone_rrset.web_access_domains_ipv4](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/zone_rrset) | resource |
| [random_bytes.admin_token](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.docker_ro_default_password](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.docker_ro_default_user](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.docker_ro_password](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.docker_rw_default_password](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.docker_rw_default_user](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.docker_rw_password](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.metrics_token](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.owner_key_ids](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.owner_secret_keys](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.ro_key_ids](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.ro_secret_keys](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.rpc_secret](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.rw_key_ids](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |
| [random_bytes.rw_secret_keys](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/bytes) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_data_volume_size"></a> [data\_volume\_size](#input\_data\_volume\_size) | data volume size in GB | `number` | `64` | no |
| <a name="input_datacenter"></a> [datacenter](#input\_datacenter) | n/a | `string` | `"nbg1-dc3"` | no |
| <a name="input_disable_volume_delete_protection"></a> [disable\_volume\_delete\_protection](#input\_disable\_volume\_delete\_protection) | n/a | `bool` | `false` | no |
| <a name="input_dns_zone"></a> [dns\_zone](#input\_dns\_zone) | DNS zone to use for hostname and DNs entries | `string` | n/a | yes |
| <a name="input_docker_enable"></a> [docker\_enable](#input\_docker\_enable) | Enable Docker registry | `bool` | `false` | no |
| <a name="input_docker_public_enable"></a> [docker\_public\_enable](#input\_docker\_public\_enable) | Enable public anonymous access to Docker registry, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#docker for details | `bool` | `false` | no |
| <a name="input_docker_ro_users"></a> [docker\_ro\_users](#input\_docker\_ro\_users) | Docker read-write users to provision. If no users is given a user will be auto.created, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#docker for details | <pre>list(object({<br/>    username : string,<br/>    password : optional(string)<br/>  }))</pre> | `[]` | no |
| <a name="input_docker_rw_users"></a> [docker\_rw\_users](#input\_docker\_rw\_users) | Docker read-write users to provision. If no users is given a user will be auto.created, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#docker for details | <pre>list(object({<br/>    username : string,<br/>    password : optional(string)<br/>  }))</pre> | `[]` | no |
| <a name="input_labels"></a> [labels](#input\_labels) | A list of labels to be attached to the server instance. | `map(any)` | `{}` | no |
| <a name="input_location"></a> [location](#input\_location) | Hetzner location to use for provisioned resources | `string` | `"nbg1"` | no |
| <a name="input_name"></a> [name](#input\_name) | Unique name for the server instance | `string` | n/a | yes |
| <a name="input_s3_buckets"></a> [s3\_buckets](#input\_s3\_buckets) | S3 buckets to provision, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#s3-buckets for details | <pre>list(object({<br/>    name                     = string<br/>    owner_key_id             = optional(string)<br/>    owner_secret_key         = optional(string)<br/>    ro_key_id                = optional(string)<br/>    ro_secret_key            = optional(string)<br/>    rw_key_id                = optional(string)<br/>    rw_secret_key            = optional(string)<br/>    web_access_public_enable = optional(bool, false)<br/>    web_access_domains       = optional(list(string))<br/>  }))</pre> | `[]` | no |
| <a name="input_server_type"></a> [server\_type](#input\_server\_type) | Hetzner cloud server type, supports x86 and ARM architectures | `string` | `"cx23"` | no |
| <a name="input_ssh_host_cert_ecdsa"></a> [ssh\_host\_cert\_ecdsa](#input\_ssh\_host\_cert\_ecdsa) | override generated ssh host ed25519 certificate, must be set alongside with 'ssh\_host\_key\_ecdsa' | `string` | `""` | no |
| <a name="input_ssh_host_cert_ed25519"></a> [ssh\_host\_cert\_ed25519](#input\_ssh\_host\_cert\_ed25519) | override generated ssh host ed25519 certificate, must be set alongside with 'ssh\_host\_key\_ed25519' | `string` | `""` | no |
| <a name="input_ssh_host_cert_rsa"></a> [ssh\_host\_cert\_rsa](#input\_ssh\_host\_cert\_rsa) | override generated ssh host ed25519 certificate, must be set alongside with 'ssh\_host\_key\_rsa' | `string` | `""` | no |
| <a name="input_ssh_host_key_ecdsa"></a> [ssh\_host\_key\_ecdsa](#input\_ssh\_host\_key\_ecdsa) | override generated ssh host ed25519 key, must be set alongside with 'ssh\_host\_cert\_ecdsa' | `string` | `""` | no |
| <a name="input_ssh_host_key_ed25519"></a> [ssh\_host\_key\_ed25519](#input\_ssh\_host\_key\_ed25519) | override generated ssh host ed25519 key, must be set alongside with 'ssh\_host\_cert\_ed25519' | `string` | `""` | no |
| <a name="input_ssh_host_key_rsa"></a> [ssh\_host\_key\_rsa](#input\_ssh\_host\_key\_rsa) | override generated ssh host ed25519 key, must be set alongside with 'ssh\_host\_cert\_rsa' | `string` | `""` | no |
| <a name="input_ssh_keys"></a> [ssh\_keys](#input\_ssh\_keys) | ssh keys to provision for instance access | `list(number)` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_docker_host_private"></a> [docker\_host\_private](#output\_docker\_host\_private) | fully qualified domain for the private docker registry |
| <a name="output_docker_host_public"></a> [docker\_host\_public](#output\_docker\_host\_public) | fully qualified domain for the public docker registry if enabled |
| <a name="output_docker_ro_users"></a> [docker\_ro\_users](#output\_docker\_ro\_users) | readonly users for the docker registry |
| <a name="output_docker_rw_users"></a> [docker\_rw\_users](#output\_docker\_rw\_users) | write users for the docker registry |
| <a name="output_garage_admin_address"></a> [garage\_admin\_address](#output\_garage\_admin\_address) | address for the GarageFS admin endpoint |
| <a name="output_garage_admin_token"></a> [garage\_admin\_token](#output\_garage\_admin\_token) | token for the GarageFS admin endpoint |
| <a name="output_ipv4_address"></a> [ipv4\_address](#output\_ipv4\_address) | IPv4 address of the created server |
| <a name="output_ipv6_address"></a> [ipv6\_address](#output\_ipv6\_address) | IPv6 address of the created server |
| <a name="output_s3_buckets"></a> [s3\_buckets](#output\_s3\_buckets) | the created S3 bucket with access credentials and public endpoints if available |
| <a name="output_s3_host"></a> [s3\_host](#output\_s3\_host) | fully qualified for the s3 endpoint |
| <a name="output_server_id"></a> [server\_id](#output\_server\_id) | Hetzner ID of the created server |
<!-- END_TF_DOCS -->