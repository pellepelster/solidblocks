+++
title = 'Network Hetzner'
description = "Provisions a Hetzner cloud network ready for a K3S deployment"
+++

Creates a private network for cluster communication and load balancers for K8S and ingress API access. See [here](/k3s/#networking) for a networking overview.

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_hcloud"></a> [hcloud](#requirement\_hcloud) | ~> 1.49 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_hcloud"></a> [hcloud](#provider\_hcloud) | ~> 1.49 |

## Resources

| Name | Type |
|------|------|
| [hcloud_firewall.k3s](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/firewall) | resource |
| [hcloud_network.network](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/network) | resource |
| [hcloud_network_subnet.load_balancers](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/network_subnet) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment"></a> [environment](#input\_environment) | Environment/stage for the resources (prod, dev, staging) | `string` | n/a | yes |
| <a name="input_labels"></a> [labels](#input\_labels) | additional labels for all created resources | `map(string)` | `{}` | no |
| <a name="input_load_balancers_subnet_cidr"></a> [load\_balancers\_subnet\_cidr](#input\_load\_balancers\_subnet\_cidr) | CIDR for the loadbalancer subnet | `string` | `"10.0.2.0/24"` | no |
| <a name="input_location"></a> [location](#input\_location) | Hetzner location for created resources (nbg1, fsn1, ...) | `string` | `"nbg1"` | no |
| <a name="input_name"></a> [name](#input\_name) | Name for the K3S cluster associated resources | `string` | n/a | yes |
| <a name="input_network_cidr"></a> [network\_cidr](#input\_network\_cidr) | CIDR for the private network | `string` | `"10.0.0.0/8"` | no |
| <a name="input_network_zone"></a> [network\_zone](#input\_network\_zone) | Hetzner network zone for network resources (eu-central, us-east, us-west) | `string` | `"eu-central"` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_network_id"></a> [network\_id](#output\_network\_id) | Hetzner resource id of the network |
<!-- END_TF_DOCS -->
