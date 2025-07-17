data "hcloud_volume" "data" {
  name = "rds-postgresql-data"
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
  source  = "pellepelster/solidblocks-rds-postgresql/hcloud"
  version = "v0.4.4"

  name     = "rds-postgresql"
  location = var.hetzner_location

  ssh_keys = [hcloud_ssh_key.ssh_key.id]

  data_volume = data.hcloud_volume.data.id

  backup_s3_bucket     = "rds-postgresql-backup"
  backup_s3_host       = "${var.hetzner_location}.your-objectstorage.com"
  backup_s3_access_key = var.backup_s3_access_key
  backup_s3_secret_key = var.backup_s3_secret_key

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
