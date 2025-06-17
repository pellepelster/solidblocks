data "hcloud_volume" "database2_green_data" {
  name = "${var.environment}-${var.name}-database2-green-data"
}

data "hcloud_volume" "database2_blue_data" {
  name = "${var.environment}-${var.name}-database2-blue-data"
}

data "hcloud_volume" "database1_blue_data" {
  name = "${var.environment}-${var.name}-database1-blue-data"
}