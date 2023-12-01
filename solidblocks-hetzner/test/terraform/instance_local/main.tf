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

  solidblocks_base_url           = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_cloud_init_version = var.solidblocks_version
  solidblocks_rds_version        = "${var.solidblocks_version}-rc"

  backup_local_retention_full_type = "count"
  backup_local_retention_full      = 6
  backup_local_retention_diff      = 2

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]

  extra_user_data = file("${path.module}/extra_user_data.sh")

  environment_variables = {
    ENV1 = "KEY1"
  }

  postgres_stop_timeout = 61
  db_admin_password     = "5aee570e-b669-4df6-b05c-1b581e88325f"
}