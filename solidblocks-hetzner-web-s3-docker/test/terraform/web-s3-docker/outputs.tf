resource "local_file" "ipv4_address" {
  content  = module.s3_docker.ipv4_address
  filename = "${path.module}/../ipv4_address"
}

output "s3_buckets" {
  sensitive = true
  value     = module.s3_docker.s3_buckets
}

output "s3_host" {
  value = module.s3_docker.s3_host
}

output "docker_host" {
  value = module.s3_docker.docker_host
}

output "docker_user" {
  value = local.docker_user
}

output "docker_password" {
  value = local.docker_password
}

output "garage_admin_address" {
  value = module.s3_docker.garage_admin_address
}

output "garage_admin_token" {
  value     = module.s3_docker.garage_admin_token
  sensitive = true
}

output "debug" {
  value = module.s3_docker.debug
}
