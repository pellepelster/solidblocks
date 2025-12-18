resource "hcloud_volume" "data" {
  name     = "test-data-${var.test_id}"
  size     = 32
  format   = "ext4"
  location = var.location
}

module "rds-postgresql-1" {
  source = "../../../modules/rds-postgresql"
  name   = "rds-postgresql-${var.test_id}"

  location = var.location
  ssh_keys = [data.hcloud_ssh_key.ssh_key.id]

  data_volume = hcloud_volume.data.id

  backup_s3_bucket     = "rds-postgresql-${var.test_id}"
  backup_s3_host       = "fsn1.your-objectstorage.com"
  backup_s3_access_key = var.hetzner_s3_access_key
  backup_s3_secret_key = var.hetzner_s3_secret_key

  solidblocks_base_url           = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_cloud_init_version = var.solidblocks_version
  solidblocks_rds_version        = "${var.solidblocks_version}-rc"

  backup_s3_retention_full_type = "count"
  backup_s3_retention_full      = 6
  backup_s3_retention_diff      = 2

  postgres_extra_config = "checkpoint_timeout = 30"
  restore_pitr          = var.restore_pitr

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}