resource "local_file" "ipv4_address" {
  content  = module.web_s3_docker.ipv4_address
  filename = "${path.module}/../ipv4_address"
}

resource "local_file" "garage_admin_address" {
  content  = module.web_s3_docker.garage_admin_address
  filename = "${path.module}/../garage_admin_address"
}

resource "local_file" "garage_admin_token" {
  content  = module.web_s3_docker.garage_admin_token
  filename = "${path.module}/../garage_admin_token"
}

output "s3_buckets" {
  sensitive = true
  value     = module.web_s3_docker.s3_buckets
}

output "s3_host" {
  value = module.web_s3_docker.s3_host
}

output "docker_host_public" {
  value = module.web_s3_docker.docker_host_public
}

output "docker_host_private" {
  value = module.web_s3_docker.docker_host_private
}

output "docker_user" {
  value = local.docker_user
}

output "docker_password" {
  value = local.docker_password
}

output "garage_admin_address" {
  value = module.web_s3_docker.garage_admin_address
}

output "garage_admin_token" {
  value     = module.web_s3_docker.garage_admin_token
  sensitive = true
}

output "docker_ro_users" {
  value     = module.web_s3_docker.docker_ro_users
  sensitive = true
}

output "docker_rw_users" {
  value     = module.web_s3_docker.docker_rw_users
  sensitive = true
}
