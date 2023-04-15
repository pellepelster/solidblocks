resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "test-${var.test_id}"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

data "aws_s3_bucket" "bootstrap" {
  bucket = "test-${var.test_id}"
}

# decouple resource so then can individually be destroyed
data "hcloud_volume" "data" {
  name = "test-data-${var.test_id}"
}

# decouple resource so then can individually be destroyed
data "hcloud_volume" "backup" {
  name = "test-backup-${var.test_id}"
}

module "rds-postgresql-1" {
  source                         = "../../../modules/rds-postgresql"
  location                       = "nbg1"
  id                             = "rds-postgresql-1"
  ssh_keys                       = [hcloud_ssh_key.ssh_key.id]
  data_volume                    = data.hcloud_volume.data.id
  backup_volume                  = data.hcloud_volume.backup.id
  solidblocks_base_url           = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_cloud_init_version = var.solidblocks_version
  db_backup_s3_bucket            = data.aws_s3_bucket.bootstrap.id
  db_backup_s3_access_key        = var.db_backup_s3_access_key
  db_backup_s3_secret_key        = var.db_backup_s3_secret_key
}