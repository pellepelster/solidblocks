data "http" "cloud_init_bootstrap_solidblocks" {
  url = "${var.solidblocks_base_url}/pellepelster/solidblocks/releases/download/${var.solidblocks_cloud_init_version}/cloud_init_bootstrap_solidblocks"
}

data "hcloud_volume" "data" {
  id = var.data_volume
}

data "hcloud_volume" "backup" {
  count = var.backup_volume > 0 ? 1 : 0
  id    = var.backup_volume
}


data "hcloud_network" "network" {
  count = var.network_id > 0 ? 1 : 0
  id    = var.network_id
}