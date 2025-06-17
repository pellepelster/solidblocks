locals {
  default_labels = {
    "blcks.de/environment" : var.environment
  }
}

module "ssh_hetzner" {
  source      = "../../../terraform/nodes-ssh-hetzner"
  environment = var.environment
  location    = var.location
  name        = var.name
  labels      = local.default_labels
}

resource "hcloud_server" "database1_blue" {
  name        = "${var.environment}-${var.name}-database1-blue"
  image       = "debian-11"
  server_type = "cx22"
  ssh_keys = [module.ssh_hetzner.root_ssh_key_id]
  location    = var.location
}

resource "hcloud_volume_attachment" "database1_blue_data" {
  server_id = hcloud_server.database1_blue.id
  volume_id = data.hcloud_volume.database1_blue_data.id
}


resource "hcloud_server" "database2_green" {
  name        = "${var.environment}-${var.name}-database2-green"
  image       = "debian-11"
  server_type = "cx22"
  ssh_keys = [module.ssh_hetzner.root_ssh_key_id]
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
  ssh_keys = [module.ssh_hetzner.root_ssh_key_id]
  location    = var.location
}

resource "hcloud_volume_attachment" "database2_blue_data" {
  server_id = hcloud_server.database2_blue.id
  volume_id = data.hcloud_volume.database2_blue_data.id
}

