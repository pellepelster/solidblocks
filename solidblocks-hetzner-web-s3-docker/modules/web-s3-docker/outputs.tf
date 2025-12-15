output "ipv4_address" {
  value       = hcloud_server.server.ipv4_address
  description = "IPv4 address of the created server if applicable"
}

output "ipv6_address" {
  value       = hcloud_server.server.ipv6_address
  description = "IPv6 address of the created server if applicable"
}

output "server_id" {
  value       = hcloud_server.server.id
  description = "Hetzner ID of the created server"
}

output "s3_host" {
  value = local.s3_api_fqdn
}

output "docker_host" {
  value = local.docker_registry_fqdn
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

output "docker_ro_users" {
  value = local.docker_ro_users
}

output "docker_rw_users" {
  value = local.docker_rw_users
}
