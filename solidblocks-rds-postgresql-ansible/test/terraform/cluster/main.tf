locals {
  default_labels = {
    "blcks.de/environment" : var.environment
  }
}

resource "hcloud_server" "database2_green" {
  name        = "${var.environment}-${var.name}-database2-green"
  image       = "debian-11"
  server_type = "cx22"
  ssh_keys = [data.hcloud_ssh_key.root_ssh_key.id]
  location    = var.location
}

resource "hcloud_volume_attachment" "database2_green_data" {
  server_id = hcloud_server.database2_green.id
  volume_id = data.hcloud_volume.database2_green_data.id
}

resource "hcloud_server" "database2_blue" {
  name        = "${var.environment}-${var.name}-database2-blue"
  image       = "debian-11"
  server_type = "cx22"
  ssh_keys = [data.hcloud_ssh_key.root_ssh_key.id]
  location    = var.location
}

resource "hcloud_volume_attachment" "database2_blue_data" {
  server_id = hcloud_server.database2_blue.id
  volume_id = data.hcloud_volume.database2_blue_data.id
}
