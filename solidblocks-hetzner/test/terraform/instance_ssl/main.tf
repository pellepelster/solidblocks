resource "hcloud_volume" "data" {
  name     = "test-data-${var.test_id}"
  size     = 32
  format   = "ext4"
  location = var.location
}


module "rds-postgresql-1" {
  source = "../../../modules/rds-postgresql"
  name   = "rds-postgresql-ssl-${var.test_id}"

  location = var.location
  ssh_keys = [data.hcloud_ssh_key.ssh_key.id]

  data_volume   = hcloud_volume.data.id
  backup_volume = data.hcloud_volume.backup.id

  ssl_enable              = true
  ssl_email               = "pelle@pelle.io"
  ssl_domains             = ["test.blcks.de"]
  ssl_dns_provider        = "hetzner"
  ssl_dns_provider_config = {
    HETZNER_API_KEY = var.hetzner_dns_api_token
  }

  solidblocks_base_url           = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_cloud_init_version = var.solidblocks_version
  solidblocks_rds_version        = "${var.solidblocks_version}-rc"

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]

}