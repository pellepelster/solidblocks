output "ingress_default_loadbalancer_ipv4_address" {
  value       = hcloud_load_balancer.k3s_ingress_default.ipv4
  description = "IpV4 address of the ingress load balancer"
}

output "k3s_api_loadbalancer_ipv4_address" {
  value       = hcloud_load_balancer.k3s_api.ipv4
  description = "IpV4 address of the K8S api load balancer"
}
