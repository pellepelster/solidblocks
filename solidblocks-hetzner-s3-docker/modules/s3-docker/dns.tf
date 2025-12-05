resource "hcloud_zone_rrset" "s3_api" {
  zone = var.dns_zone
  name = local.s3_api_domain
  type = "A"
  ttl  = 60

  records = [
    { value = hcloud_server.s3_docker.ipv4_address },
  ]
}

resource "hcloud_zone_rrset" "s3_admin" {
  zone = var.dns_zone
  name = local.s3_admin_domain
  type = "A"
  ttl  = 60

  records = [
    { value = hcloud_server.s3_docker.ipv4_address },
  ]
}

resource "hcloud_zone_rrset" "s3_web" {
  zone = var.dns_zone
  name = local.s3_web_domain
  type = "A"
  ttl  = 60

  records = [
    { value = hcloud_server.s3_docker.ipv4_address },
  ]
}

resource "hcloud_zone_rrset" "s3_web_bucket" {
  for_each     = { for s3_bucket in local.s3_buckets : s3_bucket.name => s3_bucket }

  zone     = var.dns_zone
  name     = "${each.value.name}.${local.s3_web_domain}"
  type     = "A"
  ttl      = 60

  records = [
    { value = hcloud_server.s3_docker.ipv4_address },
  ]
}

resource "hcloud_zone_rrset" "s3_api_bucket" {
  for_each     = { for s3_bucket in local.s3_buckets : s3_bucket.name => s3_bucket }

  zone     = var.dns_zone
  name     = "${each.value.name}.${local.s3_api_domain}"
  type     = "A"
  ttl      = 60

  records = [
    { value = hcloud_server.s3_docker.ipv4_address },
  ]
}
