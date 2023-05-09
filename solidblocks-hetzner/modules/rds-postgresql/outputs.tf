output "ipv4_address" {
  value       = hcloud_server.rds.ipv4_address
  description = "IpV4 address of the created server"
}

output "this_server_id" {
  value       = hcloud_server.rds.id
  description = "Hetzner ID of the created server"
}
