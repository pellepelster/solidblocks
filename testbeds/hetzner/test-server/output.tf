output "server_id" {
  value = hcloud_server.server.id
}

output "ipv4_address" {
  value = hcloud_server.server.ipv4_address
}

output "ssh_config" {
  value = local_file.ssh_public_config.filename
}