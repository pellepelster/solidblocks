resource "hcloud_zone_rrset" "root_domain_ipv4" {
  zone = var.dns_zone
  name = local.root_domain
  type = "A"
  ttl  = 60

  records = [
    { value = hcloud_server.server.ipv4_address },
  ]
}

resource "hcloud_zone_rrset" "root_domain_catchall_ipv4" {
  zone = var.dns_zone
  name = "*.${local.root_domain}"
  type = "A"
  ttl  = 60

  records = [
    { value = hcloud_server.server.ipv4_address },
  ]
}

locals {
  all_web_access_domains = flatten([for s3_bucket in local.s3_buckets : s3_bucket.web_access_domains])
}

resource "hcloud_zone_rrset" "web_access_domains_ipv4" {
  for_each = toset([for web_access_domain in local.all_web_access_domains : web_access_domain if endswith(web_access_domain, var.dns_zone)])
  zone     = var.dns_zone
  name     = trimsuffix(each.value, var.dns_zone) == "" ? "@" : trimsuffix(each.value, ".${var.dns_zone}")
  type     = "A"
  ttl      = 60

  records = [
    { value = hcloud_server.server.ipv4_address },
  ]
}

