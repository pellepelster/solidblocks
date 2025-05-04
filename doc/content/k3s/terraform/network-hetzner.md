+++
title = 'Network Hetzner'
description = "Provisions a Hetzner cloud network ready for a K3S deployment"
+++

dasdas

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
| [hcloud_load_balancer.ingress_default](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer) | resource |
| [hcloud_load_balancer.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer) | resource |
| [hcloud_load_balancer_network.ingress_default](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_network) | resource |
| [hcloud_load_balancer_network.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_network) | resource |
| [hcloud_load_balancer_service.ingress_default_http](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_service.ingress_default_https](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_service.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_service.k3s_ssh](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_target.ingress_default](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_target) | resource |
| [hcloud_load_balancer_target.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_target) | resource |
| [hcloud_network.network](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/network) | resource |
| [hcloud_network_subnet.load_balancers](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/network_subnet) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment"></a> [environment](#input\_environment) | n/a | `string` | n/a | yes |
| <a name="input_labels"></a> [labels](#input\_labels) | n/a | `map(string)` | n/a | yes |
| <a name="input_load_balancers_subnet_cidr"></a> [load\_balancers\_subnet\_cidr](#input\_load\_balancers\_subnet\_cidr) | CIDR of the loadbalancer subnet | `string` | n/a | yes |
| <a name="input_location"></a> [location](#input\_location) | n/a | `string` | n/a | yes |
| <a name="input_name"></a> [name](#input\_name) | n/a | `string` | n/a | yes |
| <a name="input_network_cidr"></a> [network\_cidr](#input\_network\_cidr) | CIDR of the private network | `string` | n/a | yes |
| <a name="input_network_zone"></a> [network\_zone](#input\_network\_zone) | network zone, eu-central, us-east, us-west. | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_ingress_default_loadbalancer_ipv4_address"></a> [ingress\_default\_loadbalancer\_ipv4\_address](#output\_ingress\_default\_loadbalancer\_ipv4\_address) | n/a |
| <a name="output_k3s_api_loadbalancer_ipv4_address"></a> [k3s\_api\_loadbalancer\_ipv4\_address](#output\_k3s\_api\_loadbalancer\_ipv4\_address) | n/a |
| <a name="output_network_id"></a> [network\_id](#output\_network\_id) | n/a |
<!-- END_TF_DOCS -->
