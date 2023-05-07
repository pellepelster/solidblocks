output "ipv4_address" {
  value = module.rds-postgresql-1.ipv4_address
}

resource "local_file" "ipv4_address" {
  content  = module.rds-postgresql-1.ipv4_address
  filename = "${path.module}/../ipv4_address"
}