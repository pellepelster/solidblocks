output "ipv4_address" {
  value       = hcloud_server.s3_docker.ipv4_address
  description = "IPv4 address of the created server if applicable"
}

output "ipv6_address" {
  value       = hcloud_server.s3_docker.ipv6_address
  description = "IPv6 address of the created server if applicable"
}

output "server_id" {
  value       = hcloud_server.s3_docker.id
  description = "Hetzner ID of the created server"
}

output "s3_api_host" {
  value = local.s3_api_fqdn
}

output "s3_buckets" {
  sensitive = true
  value = [for index, s3_bucket in local.s3_buckets : {
    name                     = s3_bucket.name
    enable_public_web_access = s3_bucket.enable_public_web_access
    web_address              = s3_bucket.enable_public_web_access ? "https://${s3_bucket.name}.${local.s3_web_fqdn}" : ""

    owner_key_id     = s3_bucket.owner_key_id
    owner_secret_key = s3_bucket.owner_secret_key

    ro_key_id     = s3_bucket.ro_key_id
    ro_secret_key = s3_bucket.ro_secret_key

    rw_key_id     = s3_bucket.rw_key_id
    rw_secret_key = s3_bucket.rw_secret_key
  }]
}

output "garage_rpc_secret" {
  value     = random_bytes.rpc_secret.hex
  sensitive = true
}

output "garage_admin_address" {
  value = "https://${local.s3_admin_domain}.${var.dns_zone}"
}

output "garage_admin_token" {
  value     = random_bytes.admin_token.hex
  sensitive = true
}

