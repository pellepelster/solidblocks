output "ipv4_address" {
  value       = hcloud_server.rds.ipv4_address
  description = "IPv4 address of the created server if applicable"
}

output "ipv6_address" {
  value       = hcloud_server.rds.ipv6_address
  description = "IPv6 address of the created server if applicable"
}

output "ipv4_address_private" {
  value       = try(one(hcloud_server.rds.network).ip, null)
  description = "private IPv4 address of the created server if applicable"
}

output "this_server_id" {
  value       = hcloud_server.rds.id
  description = "Hetzner ID of the created server"
}

output "user_data" {
  value     = local.user_data
  sensitive = false
}
