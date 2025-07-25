locals {
  default_labels = {
    "blcks.de/environment" : var.environment
  }
}

resource "hcloud_network" "network" {
  ip_range = "10.0.0.0/8"
  name     = "network"
}

resource "hcloud_network_subnet" "subnet" {
  ip_range     = "10.0.1.0/24"
  network_id   = hcloud_network.network.id
  network_zone = "eu-central"
  type         = "cloud"

}

resource "hcloud_server" "database2_green" {
  name        = "${var.environment}-${var.name}-database2-green"
  image       = "debian-11"
  server_type = "cx22"
  ssh_keys = [module.ssh_hetzner.root_ssh_key_id]
  location    = var.location
  network {
    network_id = hcloud_network.network.id
  }
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
  network {
    network_id = hcloud_network.network.id
  }
}

resource "hcloud_volume_attachment" "database2_blue_data" {
  server_id = hcloud_server.database2_blue.id
  volume_id = data.hcloud_volume.database2_blue_data.id
}

module "ssh_hetzner" {
  source      = "../../../terraform/nodes-ssh-hetzner"
  environment = var.environment
  location    = var.location
  name        = "${var.name}-standalone"
}
