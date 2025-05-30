+++
title = 'Output Ansible Hetzner'
description = "Create Ansible config file with Hetzner specifics for K3S provisioning"
+++

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
| [local_file.variables](https://registry.terraform.io/providers/hashicorp/local/latest/docs/resources/file) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_hcloud_token"></a> [hcloud\_token](#input\_hcloud\_token) | Hetzner API token with R/W access | `string` | n/a | yes |
| <a name="input_network_id"></a> [network\_id](#input\_network\_id) | Hetzner resource id of the network | `string` | n/a | yes |
| <a name="input_output_path"></a> [output\_path](#input\_output\_path) | path for the generated files | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->