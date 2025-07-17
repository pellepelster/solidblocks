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

module "ssh_hetzner" {
  source      = "../../../terraform/nodes-ssh-hetzner"
  environment = var.environment
  location    = var.location
  name        = "${var.name}-standalone"
}
