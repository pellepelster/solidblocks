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

  db_backup_gcs_bucket      = data.google_storage_bucket.backup.name
  db_backup_gcs_service_key = var.db_backup_gcs_service_key

  solidblocks_base_url    = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_rds_version = var.solidblocks_version

  backup_s3_retention_full_type = "count"
  backup_s3_retention_full      = 6
  backup_s3_retention_diff      = 2

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
