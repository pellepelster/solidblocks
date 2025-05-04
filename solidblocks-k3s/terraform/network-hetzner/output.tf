output "ingress_default_loadbalancer_ipv4_address" {
  value = hcloud_load_balancer.ingress_default.ipv4
}

output "k3s_api_loadbalancer_ipv4_address" {
  value = hcloud_load_balancer.k3s_api.ipv4
}

output "network_id" {
  value = hcloud_network.network.id
}