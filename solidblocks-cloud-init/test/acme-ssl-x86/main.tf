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

resource "hcloud_volume" "test" {
  name     = "test-x86-${random_string.test_id.id}"
  size     = 16
  format   = "ext4"
  location = local.location
}

resource "hcloud_volume_attachment" "test" {
  server_id = module.test.server_id
  volume_id = hcloud_volume.test.id
}

module "cloud_init" {
  source               = "../../modules/solidblocks-cloud-init"
  solidblocks_base_url = "https://${module.bootstrap_bucket.bucket_domain_name}"
  acme_ssl = {
    path         = "/tmp/ssl"
    email        = "contact@blcks.de"
    domains      = ["acme-ssl-x86.blcks.de"]
    acme_server  = "https://acme-staging-v02.api.letsencrypt.org/directory"
    dns_provider = "hetzner"
    variables = {
      HETZNER_API_KEY : var.hetzner_dns_api_token
      HETZNER_HTTP_TIMEOUT : "30"
      HETZNER_PROPAGATION_TIMEOUT : "300"
    }
  }
}

module "test" {
  source      = "../../../testbeds/hetzner/test-server"
  name        = "test-x86-${random_string.test_id.id}"
  server_type = "cx22"
  user_data   = module.cloud_init.user_data
}
