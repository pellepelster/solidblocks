data "hcloud_volume" "database1_blue_data" {
  name = "${var.environment}-${var.name}-database1-blue-data"
}

data "hcloud_ssh_key" "root_ssh_key" {
  name = "${var.environment}-${var.name}-root"
}
