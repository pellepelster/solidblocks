+++
title = 'Network Loadbalancer Hetzner'
description = "Provisions Loadbalancer for K3S deployments"
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

## Resources

| Name | Type |
|------|------|
| [hcloud_load_balancer.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer) | resource |
| [hcloud_load_balancer.k3s_ingress_default](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer) | resource |
| [hcloud_load_balancer_network.ingress_default](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_network) | resource |
| [hcloud_load_balancer_network.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_network) | resource |
| [hcloud_load_balancer_service.ingress_default_http](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_service.ingress_default_https](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_service.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_service) | resource |
| [hcloud_load_balancer_target.ingress_default](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_target) | resource |
| [hcloud_load_balancer_target.k3s_api](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/load_balancer_target) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment"></a> [environment](#input\_environment) | Environment/stage for the resources (prod, dev, staging) | `string` | n/a | yes |
| <a name="input_labels"></a> [labels](#input\_labels) | additional labels for all created resources | `map(string)` | `{}` | no |
| <a name="input_load_balancers_subnet_cidr"></a> [load\_balancers\_subnet\_cidr](#input\_load\_balancers\_subnet\_cidr) | CIDR for the loadbalancer subnet | `string` | `"10.0.2.0/24"` | no |
| <a name="input_load_balancers_subnet_offset"></a> [load\_balancers\_subnet\_offset](#input\_load\_balancers\_subnet\_offset) | Offset for ip allocated in the load\_balancers\_subnet\_cidr | `number` | `0` | no |
| <a name="input_location"></a> [location](#input\_location) | Hetzner location for created resources (nbg1, fsn1, ...) | `string` | `"nbg1"` | no |
| <a name="input_name"></a> [name](#input\_name) | Name for the K3S cluster associated resources | `string` | n/a | yes |
| <a name="input_network_id"></a> [network\_id](#input\_network\_id) | Hetzner network id | `number` | n/a | yes |
| <a name="input_network_zone"></a> [network\_zone](#input\_network\_zone) | Hetzner network zone for network resources (eu-central, us-east, us-west) | `string` | `"eu-central"` | no |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_ingress_default_loadbalancer_ipv4_address"></a> [ingress\_default\_loadbalancer\_ipv4\_address](#output\_ingress\_default\_loadbalancer\_ipv4\_address) | IpV4 address of the ingress load balancer |
| <a name="output_k3s_api_loadbalancer_ipv4_address"></a> [k3s\_api\_loadbalancer\_ipv4\_address](#output\_k3s\_api\_loadbalancer\_ipv4\_address) | IpV4 address of the K8S api load balancer |
<!-- END_TF_DOCS -->