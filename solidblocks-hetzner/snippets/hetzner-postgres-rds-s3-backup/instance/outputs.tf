output "ipv4_address" {
  value       = module.rds-postgresql.ipv4_address
  description = "IpV4 address of the created server"
}