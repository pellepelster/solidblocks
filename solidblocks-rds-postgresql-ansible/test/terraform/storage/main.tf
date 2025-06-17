resource "hcloud_volume" "database2_green_data" {
  name     = "${var.environment}-${var.name}-database2-green-data"
  size     = 16
  format   = "ext4"
  location = var.location
}

resource "hcloud_volume" "database2_blue_data" {
  name     = "${var.environment}-${var.name}-database2-blue-data"
  size     = 16
  format   = "ext4"
  location = var.location
}

resource "hcloud_volume" "database1_blue_data" {
  name     = "${var.environment}-${var.name}-database1-blue-data"
  size     = 16
  format   = "ext4"
  location = var.location
}
