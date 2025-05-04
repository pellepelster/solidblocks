output "ks_token" {
  value = random_string.k3s_token.id
}

output "servers" {
  value = hcloud_server.k3s_server[*]
}

output "agents" {
  value = hcloud_server.k3s_agent[*]
}