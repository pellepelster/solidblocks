data "hcloud_volume" "database1_blue_data" {
  name = "${var.environment}-${var.name}-database1-blue-data"
}
