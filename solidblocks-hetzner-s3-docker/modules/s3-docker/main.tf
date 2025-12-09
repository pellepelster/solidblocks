resource "random_bytes" "rpc_secret" {
  length = 32
}

resource "random_bytes" "admin_token" {
  length = 32
}

resource "random_bytes" "metrics_token" {
  length = 32
}

resource "random_bytes" "owner_secret_keys" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 32
}

resource "random_bytes" "owner_key_ids" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 12
}

resource "random_bytes" "ro_secret_keys" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 32
}

resource "random_bytes" "ro_key_ids" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 12
}

resource "random_bytes" "rw_secret_keys" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 32
}

resource "random_bytes" "rw_key_ids" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 12
}

locals {

  s3_api_domain   = "${var.name}-s3-api"
  s3_web_domain   = "${var.name}-s3-web"
  s3_admin_domain = "${var.name}-s3-admin"

  s3_api_fqdn   = "${local.s3_api_domain}.${var.dns_zone}"
  s3_web_fqdn   = "${local.s3_web_domain}.${var.dns_zone}"
  s3_admin_fqdn = "${local.s3_admin_domain}.${var.dns_zone}"

  docker_registry_domain = "${var.name}-docker"
  docker_registry_fqdn   = "${local.docker_registry_domain}.${var.dns_zone}"

  s3_buckets = [for index, s3_bucket in var.s3_buckets : {
    name                     = s3_bucket.name
    enable_public_web_access = s3_bucket.enable_public_web_access

    owner_key_id     = "GK${s3_bucket.owner_key_id == null ? random_bytes.owner_key_ids[s3_bucket.name].hex : s3_bucket.owner_key_id}"
    owner_secret_key = s3_bucket.owner_secret_key == null ? random_bytes.owner_secret_keys[s3_bucket.name].hex : s3_bucket.owner_secret_key

    rw_key_id     = "GK${s3_bucket.rw_key_id == null ? random_bytes.rw_key_ids[s3_bucket.name].hex : s3_bucket.rw_key_id}"
    rw_secret_key = s3_bucket.rw_secret_key == null ? random_bytes.rw_secret_keys[s3_bucket.name].hex : s3_bucket.rw_secret_key

    ro_key_id     = "GK${s3_bucket.ro_key_id == null ? random_bytes.ro_key_ids[s3_bucket.name].hex : s3_bucket.ro_key_id}"
    ro_secret_key = s3_bucket.ro_secret_key == null ? random_bytes.ro_secret_keys[s3_bucket.name].hex : s3_bucket.ro_secret_key
  }]

  user_data = templatefile("${path.module}/user_data_skeleton.sh", {

    caddy_lib               = file("${path.module}/caddy_lib.sh")
    storage_lib             = file("${path.module}/storage_lib.sh")
    apt_lib                 = file("${path.module}/apt_lib.sh")
    docker_registry_lib     = file("${path.module}/docker_registry_lib.sh")
    garage_lib              = file("${path.module}/garage_lib.sh")
    s3_buckets_json_base64  = base64encode(jsonencode(local.s3_buckets))
    garage_py_base64        = base64encode(file("${path.module}/garage.py"))
    requirements_txt_base64 = base64encode(file("${path.module}/requirements.txt"))

    caddy = templatefile("${path.module}/caddy.sh", {
      docker_users = var.docker_users
      s3_buckets   = local.s3_buckets
    })

    garage = templatefile("${path.module}/garage.sh", {
      rpc_secret    = random_bytes.rpc_secret.hex
      admin_token   = random_bytes.admin_token.hex
      metrics_token = random_bytes.metrics_token.hex
    })

    variables = templatefile("${path.module}/variables.sh", {
      s3_api_fqdn               = local.s3_api_fqdn
      s3_web_fqdn               = local.s3_web_fqdn
      s3_admin_fqdn             = local.s3_admin_fqdn
      docker_registry_fqdn      = local.docker_registry_fqdn
      blcks_storage_device_data = hcloud_volume.data.linux_device
    })

    s3_buckets          = local.s3_buckets
    allow_public_access = var.allow_public_access
    data_volume_size    = var.data_volume_size
  })
}

resource "hcloud_server" "s3_docker" {
  name        = var.name
  image       = "debian-13"
  server_type = var.server_type
  ssh_keys    = var.ssh_keys
  location    = var.location

  user_data = local.user_data

  public_net {
    ipv4_enabled = true
    ipv6_enabled = true
  }

  labels = var.labels
}

resource "hcloud_volume" "data" {
  name     = "${var.name}-data"
  size     = var.data_volume_size
  location = var.location
  format   = "ext4"
}

resource "hcloud_volume_attachment" "data" {
  server_id = hcloud_server.s3_docker.id
  volume_id = hcloud_volume.data.id
}
