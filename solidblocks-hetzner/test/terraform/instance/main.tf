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

resource hcloud_volume "data" {
  name     = "test-data-${var.test_id}"
  size     = 32
  format   = "ext4"
  location = var.location
}

module "rds-postgresql-1" {
  source = "../../../modules/rds-postgresql"
  name   = "rds-postgresql-1"

  location = "nbg1"
  ssh_keys = [hcloud_ssh_key.ssh_key.id]

  data_volume = hcloud_volume.data.id

  backup_s3_bucket     = data.aws_s3_bucket.bootstrap.id
  backup_s3_access_key = var.backup_s3_access_key
  backup_s3_secret_key = var.backup_s3_secret_key

  solidblocks_base_url           = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_cloud_init_version = var.solidblocks_version
}