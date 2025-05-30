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
| <a name="input_cluster_cidr"></a> [cluster\_cidr](#input\_cluster\_cidr) | CIDR for the K3S cluster, see https://github.com/hetznercloud/hcloud-cloud-controller-manager/blob/main/docs/deploy_with_networks.md#considerations-on-the-ip-ranges | `string` | `"10.0.16.0/20"` | no |
| <a name="input_environment"></a> [environment](#input\_environment) | Environment/stage for the resources (prod, dev, staging) | `string` | n/a | yes |
| <a name="input_k3s_agents"></a> [k3s\_agents](#input\_k3s\_agents) | K3S agents | `list(object({ name = string, ipv4_address = string }))` | n/a | yes |
| <a name="input_k3s_api_endpoint"></a> [k3s\_api\_endpoint](#input\_k3s\_api\_endpoint) | n/a | `string` | `"public address of the K3S api endpoint"` | no |
| <a name="input_k3s_api_endpoint_ip"></a> [k3s\_api\_endpoint\_ip](#input\_k3s\_api\_endpoint\_ip) | n/a | `string` | `"ip address of the K3S api endpoint"` | no |
| <a name="input_k3s_servers"></a> [k3s\_servers](#input\_k3s\_servers) | K3S servers | `list(object({ name = string, ipv4_address = string }))` | n/a | yes |
| <a name="input_k3s_token"></a> [k3s\_token](#input\_k3s\_token) | K3S token, see https://docs.k3s.io/cli/token | `string` | n/a | yes |
| <a name="input_name"></a> [name](#input\_name) | Name for the K3S cluster associated resources | `string` | n/a | yes |
| <a name="input_network_cidr"></a> [network\_cidr](#input\_network\_cidr) | Hetzner id of the cluster network | `string` | n/a | yes |
| <a name="input_nodes_cidr"></a> [nodes\_cidr](#input\_nodes\_cidr) | CIDR for the K3S cluster nodes (servers and agents) | `string` | n/a | yes |
| <a name="input_output_path"></a> [output\_path](#input\_output\_path) | path for the generated files | `string` | n/a | yes |
| <a name="input_service_cidr"></a> [service\_cidr](#input\_service\_cidr) | CIDR for the K3S cluster services, see https://github.com/hetznercloud/hcloud-cloud-controller-manager/blob/main/docs/deploy_with_networks.md#considerations-on-the-ip-ranges | `string` | `"10.0.8.0/21"` | no |
| <a name="input_ssh_config_file"></a> [ssh\_config\_file](#input\_ssh\_config\_file) | path to the ssh client config file for K3S node access | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->