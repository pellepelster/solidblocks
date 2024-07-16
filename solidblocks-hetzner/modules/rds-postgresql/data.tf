data "http" "cloud_init_bootstrap_solidblocks" {
  url = "${var.solidblocks_base_url}/pellepelster/solidblocks/releases/download/${var.solidblocks_cloud_init_version}/solidblocks-cloud-init-bootstrap.sh"
}

data "hcloud_volume" "data" {
  id = var.data_volume
}

data "hcloud_volume" "backup" {
  count = var.backup_volume > 0 ? 1 : 0
  id    = var.backup_volume
}
