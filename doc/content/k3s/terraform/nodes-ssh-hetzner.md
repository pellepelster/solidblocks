+++
title = 'Nodes SSH Hetzner'
description = "Hetzner SSH keys for VMs access"
+++

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_hcloud"></a> [hcloud](#requirement\_hcloud) | ~> 1.49 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_hcloud"></a> [hcloud](#provider\_hcloud) | ~> 1.49 |
| <a name="provider_tls"></a> [tls](#provider\_tls) | n/a |

## Resources

| Name | Type |
|------|------|
| [hcloud_ssh_key.root](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/ssh_key) | resource |
| [tls_private_key.ssh_client_identity](https://registry.terraform.io/providers/hashicorp/tls/latest/docs/resources/private_key) | resource |
| [tls_private_key.ssh_host_identity](https://registry.terraform.io/providers/hashicorp/tls/latest/docs/resources/private_key) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment"></a> [environment](#input\_environment) | n/a | `string` | n/a | yes |
| <a name="input_labels"></a> [labels](#input\_labels) | n/a | `map(string)` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | n/a | yes |
| <a name="input_name"></a> [name](#input\_name) | n/a | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_root_ssh_key_id"></a> [root\_ssh\_key\_id](#output\_root\_ssh\_key\_id) | n/a |
| <a name="output_root_ssh_key_openssh_public"></a> [root\_ssh\_key\_openssh\_public](#output\_root\_ssh\_key\_openssh\_public) | n/a |
| <a name="output_ssh_private_key_openssh"></a> [ssh\_private\_key\_openssh](#output\_ssh\_private\_key\_openssh) | n/a |
<!-- END_TF_DOCS -->