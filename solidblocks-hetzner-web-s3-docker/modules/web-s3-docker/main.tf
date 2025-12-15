locals {

  root_domain_parts = [var.name]
  root_domain       = join(".", local.root_domain_parts)

  s3_api_domain_parts = concat(["s3"], local.root_domain_parts)
  s3_api_domain       = join(".", local.s3_api_domain_parts)
  s3_api_fqdn         = "${local.s3_api_domain}.${var.dns_zone}"

  s3_web_domain_parts = concat(["s3-web"], local.root_domain_parts)
  s3_web_domain       = join(".", local.s3_web_domain_parts)
  s3_web_fqdn         = "${local.s3_web_domain}.${var.dns_zone}"

  s3_admin_domain_parts = concat(["s3-admin"], local.root_domain_parts)
  s3_admin_domain       = join(".", local.s3_admin_domain_parts)
  s3_admin_fqdn         = "${local.s3_admin_domain}.${var.dns_zone}"

  docker_domain_parts    = concat(["docker"], local.root_domain_parts)
  docker_registry_domain = join(".", local.docker_domain_parts)
  docker_registry_fqdn   = "${local.docker_registry_domain}.${var.dns_zone}"

  s3_buckets = [for index, s3_bucket in var.s3_buckets : {
    name                     = s3_bucket.name
    web_access_domains       = s3_bucket.web_access_domains == null ? [] : s3_bucket.web_access_domains
    web_access_public_enable = s3_bucket.web_access_public_enable

    owner_key_id     = "GK${s3_bucket.owner_key_id == null ? random_bytes.owner_key_ids[s3_bucket.name].hex : s3_bucket.owner_key_id}"
    owner_secret_key = s3_bucket.owner_secret_key == null ? random_bytes.owner_secret_keys[s3_bucket.name].hex : s3_bucket.owner_secret_key

    rw_key_id     = "GK${s3_bucket.rw_key_id == null ? random_bytes.rw_key_ids[s3_bucket.name].hex : s3_bucket.rw_key_id}"
    rw_secret_key = s3_bucket.rw_secret_key == null ? random_bytes.rw_secret_keys[s3_bucket.name].hex : s3_bucket.rw_secret_key

    ro_key_id     = "GK${s3_bucket.ro_key_id == null ? random_bytes.ro_key_ids[s3_bucket.name].hex : s3_bucket.ro_key_id}"
    ro_secret_key = s3_bucket.ro_secret_key == null ? random_bytes.ro_secret_keys[s3_bucket.name].hex : s3_bucket.ro_secret_key
  }]

  docker_ro_users = [for index, user in var.docker_ro_users : {
    username = user.username
    password = user.password == null ? random_bytes.docker_ro_password[user.username].hex : user.password
  }]

  docker_rw_users = [for index, user in var.docker_rw_users : {
    username = user.username
    password = user.password == null ? random_bytes.docker_rw_password[user.username].hex : user.password
  }]

  user_data = templatefile("${path.module}/user_data_skeleton.sh", {

    caddy_lib               = file("${path.module}/caddy_lib.sh")
    storage_lib             = file("${path.module}/storage_lib.sh")
    apt_lib                 = file("${path.module}/apt_lib.sh")
    docker_registry_lib     = file("${path.module}/docker_registry_lib.sh")
    garage_lib              = file("${path.module}/garage_lib.sh")
    user_data_lib           = file("${path.module}/user_data_lib.sh")
    s3_buckets_json_base64  = base64encode(jsonencode(local.s3_buckets))
    garage_py_base64        = base64encode(file("${path.module}/garage.py"))
    requirements_txt_base64 = base64encode(file("${path.module}/requirements.txt"))

    caddy = templatefile("${path.module}/caddy.sh", {
      docker_ro_users = local.docker_ro_users
      docker_rw_users = local.docker_rw_users
      s3_buckets      = local.s3_buckets
    })

    garage = templatefile("${path.module}/garage.sh", {
      rpc_secret    = random_bytes.rpc_secret.hex
      admin_token   = random_bytes.admin_token.hex
      metrics_token = random_bytes.metrics_token.hex
    })

    variables = templatefile("${path.module}/variables.sh", {
      s3_api_fqdn               = local.s3_api_fqdn
      s3_web_fqdn               = local.s3_web_fqdn
      dns_zone                  = var.dns_zone
      s3_admin_fqdn             = local.s3_admin_fqdn
      docker_registry_fqdn      = local.docker_registry_fqdn
      blcks_storage_device_data = hcloud_volume.data.linux_device
    })

    s3_buckets          = local.s3_buckets
    allow_public_access = var.allow_public_access
    data_volume_size    = var.data_volume_size
  })
}

resource "hcloud_server" "server" {
  name        = var.name
  image       = "debian-13"
  server_type = var.server_type
  ssh_keys    = var.ssh_keys
  location    = var.location

  user_data = local.user_data

  public_net {
    ipv4 = hcloud_primary_ip.ipv4.id
    ipv6 = hcloud_primary_ip.ipv6.id
  }

  labels = var.labels
}

resource "hcloud_volume" "data" {
  name              = "${var.name}-data"
  size              = var.data_volume_size
  location          = var.location
  format            = "ext4"
  delete_protection = true
}

resource "hcloud_volume_attachment" "data" {
  server_id = hcloud_server.server.id
  volume_id = hcloud_volume.data.id
}
