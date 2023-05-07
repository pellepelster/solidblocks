resource hcloud_volume "data" {
  name     = "rds-postgresql-data"
  size     = 32
  format   = "ext4"
  location = var.hetzner_location
}

resource hcloud_volume "backup" {
  name     = "rds-postgresql-backup"
  size     = 32
  format   = "ext4"
  location = var.hetzner_location
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "rds-postgresql"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

module "rds-postgresql" {
  source = "github.com/pellepelster/solidblocks//solidblocks-hetzner/modules/rds-postgresql"

  name     = "rds-postgresql"
  location = var.hetzner_location

  ssh_keys = [hcloud_ssh_key.ssh_key.id]

  data_volume   = hcloud_volume.data.id
  backup_volume = hcloud_volume.backup.id

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}