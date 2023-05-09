output "ipv4_address" {
  value = hcloud_server.rds.ipv4_address
}

output "this_server_id" {
  value = hcloud_server.rds.id
}
