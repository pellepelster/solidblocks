resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "rds-postgresql"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

data "hcloud_volume" "data" {
  name = "rds-postgresql-data"
}

data "hcloud_volume" "backup" {
  name = "rds-postgresql-backup"
}

resource "hcloud_network" "network" {
  ip_range = "10.0.0.0/16"
  name     = "network"
}

resource "hcloud_network_subnet" "subnet" {
  ip_range     = "10.0.1.0/24"
  network_id   = hcloud_network.network.id
  network_zone = "eu-central"
  type         = "cloud"
}

module "rds-postgresql" {
  source  = "pellepelster/solidblocks-rds-postgresql/hcloud"
  version = "0.2.8-pre2"

  name     = "rds-postgresql"
  location = var.hetzner_location
  ssh_keys = [hcloud_ssh_key.ssh_key.id]

  backup_volume = data.hcloud_volume.backup.id
  data_volume   = data.hcloud_volume.data.id

  public_net_ipv4_enabled = false
  public_net_ipv6_enabled = false
  network_id              = hcloud_network.network.id
  network_ip              = "10.0.1.5"

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
