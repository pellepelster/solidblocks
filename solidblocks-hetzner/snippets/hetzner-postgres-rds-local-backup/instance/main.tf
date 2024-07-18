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

module "rds-postgresql" {
  source  = "pellepelster/solidblocks-rds-postgresql/hcloud"
  version = "1.2.6-pre3"
  name     = "rds-postgresql"
  location = var.hetzner_location
  ssh_keys = [hcloud_ssh_key.ssh_key.id]

  backup_volume = data.hcloud_volume.backup.id
  data_volume   = data.hcloud_volume.data.id

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
