output "ipv4_address" {
  value       = hcloud_server.server.ipv4_address
  description = "IPv4 address of the created server"
}

output "ipv6_address" {
  value       = hcloud_server.server.ipv6_address
  description = "IPv6 address of the created server"
}

output "server_id" {
  value       = hcloud_server.server.id
  description = "Hetzner ID of the created server"
}

output "s3_host" {
  value       = local.s3_api_fqdn
  description = "fully qualified for the s3 endpoint"
}

output "docker_host_private" {
  value       = local.docker_registry_private_fqdn
  description = "fully qualified domain for the private docker registry"
}

output "docker_host_public" {
  value       = var.docker_public_enable ? local.docker_registry_public_fqdn : null
  description = "fully qualified domain for the public docker registry if enabled"
}

output "s3_buckets" {
  sensitive = true
  value = [for index, s3_bucket in local.s3_buckets : {
    name                     = s3_bucket.name
    web_access_public_enable = s3_bucket.web_access_public_enable
    web_access_addresses = s3_bucket.web_access_public_enable ? concat(
      [for web_access_domain in s3_bucket.web_access_domains : "https://${web_access_domain}"],
      ["https://${s3_bucket.name}.${local.s3_web_fqdn}"]
    ) : []

    owner_key_id     = s3_bucket.owner_key_id
    owner_secret_key = s3_bucket.owner_secret_key

    ro_key_id     = s3_bucket.ro_key_id
    ro_secret_key = s3_bucket.ro_secret_key

    rw_key_id     = s3_bucket.rw_key_id
    rw_secret_key = s3_bucket.rw_secret_key
  }]
  description = "the created S3 bucket with access credentials and public endpoints if available"
}

output "garage_admin_address" {
  value       = "https://${local.s3_admin_domain}.${var.dns_zone}"
  description = "address for the GarageFS admin endpoint"
}

output "garage_admin_token" {
  value       = random_bytes.admin_token.hex
  sensitive   = true
  description = "token for the GarageFS admin endpoint"
}

output "docker_ro_users" {
  value       = [{ username = random_bytes.docker_ro_default_user.hex, password = random_bytes.docker_ro_default_password.hex }]
  description = "readonly users for the docker registry"
}

output "docker_rw_users" {
  value       = [{ username = random_bytes.docker_rw_default_user.hex, password = random_bytes.docker_rw_default_password.hex }]
  description = "write users for the docker registry"
}
