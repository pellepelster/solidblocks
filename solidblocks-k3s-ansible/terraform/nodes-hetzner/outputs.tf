output "ks_token" {
  value       = random_string.k3s_token.id
  description = "K3S token, see https://docs.k3s.io/cli/token"
}

output "servers" {
  value       = hcloud_server.k3s_server[*]
  description = "list if created K3S servers"
}

output "agents" {
  value       = hcloud_server.k3s_agent[*]
  description = "list if created K3S agents"
}