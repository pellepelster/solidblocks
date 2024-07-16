locals {
  location = "nbg1"
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

module "test" {
  source      = "../../../testbeds/hetzner/test-server"
  name        = "test-x86-${random_string.test_id.id}"
  server_type = "cx11"
  user_data   = templatefile("${path.module}/cloud_init.sh", {
    solidblocks_base_url   = "https://${module.bootstrap_bucket.bucket_domain_name}"
    cloud_minimal_skeleton = file("${path.module}/../../build/snippets/cloud_init_minimal_skeleton")
    storage_device         = hcloud_volume.test.linux_device
    hetzner_dns_api_token  = var.hetzner_dns_api_token
    ssl_domain             = "x86.blcks.de"
  })
}
