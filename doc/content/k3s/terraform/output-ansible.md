+++
title = 'Output Ansible'
description = "Create Ansible inventory and config file for K3S provisioning"
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
| [local_file.inventory](https://registry.terraform.io/providers/hashicorp/local/latest/docs/resources/file) | resource |
| [local_file.variables](https://registry.terraform.io/providers/hashicorp/local/latest/docs/resources/file) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_cluster_cidr"></a> [cluster\_cidr](#input\_cluster\_cidr) | n/a | `string` | n/a | yes |
| <a name="input_environment"></a> [environment](#input\_environment) | n/a | `string` | n/a | yes |
| <a name="input_k3s_agents"></a> [k3s\_agents](#input\_k3s\_agents) | n/a | `list(object({ name = string, ipv4_address = string }))` | n/a | yes |
| <a name="input_k3s_api_endpoint"></a> [k3s\_api\_endpoint](#input\_k3s\_api\_endpoint) | n/a | `string` | n/a | yes |
| <a name="input_k3s_api_endpoint_ip"></a> [k3s\_api\_endpoint\_ip](#input\_k3s\_api\_endpoint\_ip) | n/a | `string` | n/a | yes |
| <a name="input_k3s_servers"></a> [k3s\_servers](#input\_k3s\_servers) | n/a | `list(object({ name = string, ipv4_address = string }))` | n/a | yes |
| <a name="input_k3s_token"></a> [k3s\_token](#input\_k3s\_token) | n/a | `string` | n/a | yes |
| <a name="input_name"></a> [name](#input\_name) | n/a | `string` | n/a | yes |
| <a name="input_network_cidr"></a> [network\_cidr](#input\_network\_cidr) | n/a | `string` | n/a | yes |
| <a name="input_nodes_cidr"></a> [nodes\_cidr](#input\_nodes\_cidr) | n/a | `string` | n/a | yes |
| <a name="input_output_path"></a> [output\_path](#input\_output\_path) | n/a | `string` | n/a | yes |
| <a name="input_service_cidr"></a> [service\_cidr](#input\_service\_cidr) | n/a | `string` | n/a | yes |
| <a name="input_ssh_config_file"></a> [ssh\_config\_file](#input\_ssh\_config\_file) | n/a | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->