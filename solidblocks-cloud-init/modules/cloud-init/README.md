# RDS PostgreSQL

See [documentation](https://pellepelster.github.io/solidblocks/cloud-init) for more details and usage examples.

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_local"></a> [local](#provider\_local) | n/a |

## Resources

| Name | Type |
|------|------|
| [local_file.cloud_init_header](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |
| [local_file.solidblocks_cloud_init_bootstrap](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_acme_ssl"></a> [acme\_ssl](#input\_acme\_ssl) | Configure ACME certificate retrieval from LetsEncrypt, see [documentation](https://pellepelster.github.io/solidblocks/cloud-init/functions/lego/#lego_setup_dns) | <pre>object({<br>    path         = string<br>    email        = string<br>    domains      = list(string)<br>    acme_server  = string<br>    dns_provider = string<br>    variables    = map(string)<br>  })</pre> | `null` | no |
| <a name="input_solidblocks_base_url"></a> [solidblocks\_base\_url](#input\_solidblocks\_base\_url) | Override base url for bootstrapping Solidblocks cloud-init for integration testing purposes | `string` | `null` | no |
| <a name="input_storage"></a> [storage](#input\_storage) | List of `linux_device`s to mount into `mount_path`s, see [documentation](https://pellepelster.github.io/solidblocks/cloud-init/functions/storage/#storage_mount) | <pre>list(object({<br>    linux_device = string<br>    mount_path   = string<br>  }))</pre> | `[]` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_user_data"></a> [user\_data](#output\_user\_data) | The compiled cloud-init user data script |
<!-- END_TF_DOCS -->