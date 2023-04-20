data "http" "cloud_init_bootstrap_solidblocks" {
  url = "${var.solidblocks_base_url}/pellepelster/solidblocks/releases/download/${var.solidblocks_cloud_init_version}/cloud_init_bootstrap_solidblocks"
}

data hcloud_volume data {
  id = var.data_volume
}

data hcloud_volume backup {
  count = var.backup_volume > 0 ? 1 : 0
  id    = var.backup_volume
}

resource hcloud_server "rds" {
  name        = var.name
  image       = "debian-11"
  server_type = "cx11"
  ssh_keys    = var.ssh_keys
  location    = var.location

  user_data = templatefile("${path.module}/user_data.sh", {
    db_instance_name                 = var.name
    solidblocks_version              = var.solidblocks_version
    solidblocks_base_url             = var.solidblocks_base_url
    storage_device_data              = data.hcloud_volume.data.linux_device
    storage_device_backup            = try(data.hcloud_volume.backup[0].linux_device, "")
    cloud_init_bootstrap_solidblocks = data.http.cloud_init_bootstrap_solidblocks.response_body

    db_backup_s3_bucket     = var.backup_s3_bucket
    db_backup_s3_access_key = var.backup_s3_access_key
    db_backup_s3_secret_key = var.backup_s3_secret_key
  })
}

resource hcloud_volume_attachment data {
  server_id = hcloud_server.rds.id
  volume_id = var.data_volume
}

resource hcloud_volume_attachment backup {
  count     = var.backup_volume > 0 ? 1 : 0
  server_id = hcloud_server.rds.id
  volume_id = var.backup_volume
}
