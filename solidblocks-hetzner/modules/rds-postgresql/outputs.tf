output "ipv4_address" {
  value       = hcloud_server.rds.ipv4_address
  description = "IpV4 address of the created server"
}