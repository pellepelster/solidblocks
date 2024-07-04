locals {
  location = "nbg1"
}

module "bootstrap_bucket" {
  source = "../../../testbeds/hetzner/bootstrap-bucket"
}

resource "random_string" "test" {
  length  = 16
  special = false
  lower   = true
  upper   = false
}

resource "hcloud_volume" "test" {
  name     = "test-x86-${random_string.test.id}"
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
  name        = "test-arm-${random_string.test.id}"
  server_type = "cax11"
  user_data   = templatefile("${path.module}/cloud_init.sh", {
    solidblocks_base_url   = "https://${module.bootstrap_bucket.bucket_domain_name}"
    cloud_minimal_skeleton = file("${path.module}/../../build/snippets/cloud_init_minimal_skeleton")
    storage_device         = hcloud_volume.test.linux_device
    hetzner_dns_api_token  = var.hetzner_dns_api_token
    ssl_domain             = "arm.blcks.de"
  })
}

resource "aws_s3_object" "bootstrap" {
  bucket = module.bootstrap_bucket.bucket_id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  source = "${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  etag   = filemd5("${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip")
}