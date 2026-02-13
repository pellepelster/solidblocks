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

  server_type = "cax11"
  data_volume = hcloud_volume.data.id

  backup_s3_bucket     = data.aws_s3_bucket.bootstrap.id
  backup_s3_access_key = var.backup_s3_access_key
  backup_s3_secret_key = var.backup_s3_secret_key

  solidblocks_base_url    = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_rds_version = var.solidblocks_rds_version

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
