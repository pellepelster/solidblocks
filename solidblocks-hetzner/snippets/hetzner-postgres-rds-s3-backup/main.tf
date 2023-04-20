resource "aws_s3_bucket" "backup" {
  bucket        = "test-rds-postgresql-backup"
  force_destroy = true
}

resource hcloud_volume "data" {
  name     = "rds-postgresql"
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

  data_volume = hcloud_volume.data.id

  backup_s3_bucket     = aws_s3_bucket.backup.id
  backup_s3_access_key = var.backup_s3_access_key
  backup_s3_secret_key = var.backup_s3_secret_key
}