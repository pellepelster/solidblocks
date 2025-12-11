locals {
  location = "hel1"
}

resource "random_string" "test_id" {
  length  = 16
  special = false
  lower   = true
  upper   = false
}

module "bootstrap_bucket" {
  source              = "../bootstrap-bucket"
  solidblocks_version = var.solidblocks_version
  test_id             = random_string.test_id.id
}

module "cloud_init" {
  source = "../../modules/solidblocks-cloud-init"
  solidblocks_base_url = "https://${module.bootstrap_bucket.bucket_domain_name}"
}

module "test" {
  source      = "../../../testbeds/hetzner/test-server"
  name        = "test-${random_string.test_id.id}"
  server_type = "cx23"
  user_data   = module.cloud_init.user_data
}
