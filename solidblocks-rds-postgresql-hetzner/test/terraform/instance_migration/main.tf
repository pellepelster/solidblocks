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

  data_volume   = hcloud_volume.data.id
  backup_volume = data.hcloud_volume.backup.id

  solidblocks_base_url    = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_rds_version = var.solidblocks_rds_version

  postgres_major_version = var.postgres_major_version

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]
}
