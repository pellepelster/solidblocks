output "ipv4_address" {
  value       = coalescelist(hcloud_server.rds_private, hcloud_server.rds_public)[0].ipv4_address
  description = "IpV4 address of the created server if applicable"
}

output "ipv4_address_private" {
  value       = try(one(hcloud_server.rds_private[0].network).ip, null)
  description = "private IpV4 address of the created server if applicable"
}

output "this_server_id" {
  value       = coalescelist(hcloud_server.rds_private, hcloud_server.rds_public)[0].id
  description = "Hetzner ID of the created server"
}

output "user_data" {
  value     = local.user_data
  sensitive = false
}
