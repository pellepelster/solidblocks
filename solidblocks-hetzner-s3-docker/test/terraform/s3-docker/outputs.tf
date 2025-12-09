resource "local_file" "ipv4_address" {
  content  = module.s3_docker.ipv4_address
  filename = "${path.module}/../ipv4_address"
}

output "s3_buckets" {
  sensitive = true
  value     = module.s3_docker.s3_buckets
}

output "s3_api_host" {
  value = module.s3_docker.s3_api_host
}

output "garage_admin_address" {
  value = module.s3_docker.garage_admin_address
}

output "garage_admin_token" {
  value     = module.s3_docker.garage_admin_token
  sensitive = true
}

