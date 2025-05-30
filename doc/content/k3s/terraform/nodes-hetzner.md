+++
title = 'Nodes Hetzner'
description = "Provisions a VMs for K3S nodes"
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
| <a name="provider_random"></a> [random](#provider\_random) | n/a |

## Resources

| Name | Type |
|------|------|
| [hcloud_network_subnet.k3s_nodes](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/network_subnet) | resource |
| [hcloud_server.k3s_agent](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server) | resource |
| [hcloud_server.k3s_server](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server) | resource |
| [hcloud_volume.k3s_server_data](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/volume) | resource |
| [hcloud_volume_attachment.k3s_server_data](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/volume_attachment) | resource |
| [random_string.k3s_token](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/string) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_agent_count"></a> [agent\_count](#input\_agent\_count) | Number of K3S agents to create | `number` | `1` | no |
| <a name="input_agent_type"></a> [agent\_type](#input\_agent\_type) | Hetzner server instance type for K3S agents | `string` | `"ccx23"` | no |
| <a name="input_environment"></a> [environment](#input\_environment) | Environment/stage for the resources (prod, dev, staging) | `string` | n/a | yes |
| <a name="input_labels"></a> [labels](#input\_labels) | additional labels for all created resources | `map(string)` | `{}` | no |
| <a name="input_location"></a> [location](#input\_location) | Hetzner location for created resources (nbg1, fsn1, ...) | `string` | `"nbg1"` | no |
| <a name="input_name"></a> [name](#input\_name) | Name for the K3S cluster associated resources | `string` | n/a | yes |
| <a name="input_network_id"></a> [network\_id](#input\_network\_id) | Hetzner network id for the created servers | `number` | n/a | yes |
| <a name="input_network_zone"></a> [network\_zone](#input\_network\_zone) | Hetzner network zone for network resources (eu-central, us-east, us-west) | `string` | `"eu-central"` | no |
| <a name="input_nodes_cidr"></a> [nodes\_cidr](#input\_nodes\_cidr) | CIDR for the server subnet | `string` | `"10.0.1.0/24"` | no |
| <a name="input_server_count"></a> [server\_count](#input\_server\_count) | Number of K3S servers to create | `number` | `3` | no |
| <a name="input_server_type"></a> [server\_type](#input\_server\_type) | Hetzner server instance type for K3S servers | `string` | `"ccx13"` | no |
| <a name="input_ssh_key_id"></a> [ssh\_key\_id](#input\_ssh\_key\_id) | Hetzner id of the SSH key for the created servers | `number` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_agents"></a> [agents](#output\_agents) | list if created K3S agents |
| <a name="output_ks_token"></a> [ks\_token](#output\_ks\_token) | K3S token, see https://docs.k3s.io/cli/token |
| <a name="output_servers"></a> [servers](#output\_servers) | list if created K3S servers |
<!-- END_TF_DOCS -->