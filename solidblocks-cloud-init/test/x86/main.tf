locals {
  location = "nbg1"
}

module "bootstrap_bucket" {
  source = "../../../testbeds/hetzner/bootstrap-bucket"
}

resource "random_string" "test_id" {
  length  = 16
  special = false
  lower   = true
  upper   = false
}

resource "hcloud_volume" "test_x86" {
  name     = "test-x86-${random_string.test_id.id}"
  size     = 16
  format   = "ext4"
  location = local.location
}

resource "hcloud_volume" "test_arm" {
  name     = "test-arm-${random_string.test_id.id}"
  size     = 16
  format   = "ext4"
  location = local.location
}

resource "hcloud_volume_attachment" "test_x86" {
  server_id = module.test_x86.server_id
  volume_id = hcloud_volume.test_x86.id
}

resource "hcloud_volume_attachment" "test_arm" {
  server_id = module.test_arm.server_id
  volume_id = hcloud_volume.test_arm.id
}

module "test_x86" {
  source      = "../../../testbeds/hetzner/test-server"
  name        = "test-x86"
  server_type = "cx11"
  user_data   = templatefile("${path.module}/cloud_init.sh", {
    solidblocks_base_url   = "https://${module.bootstrap_bucket.bucket_domain_name}"
    cloud_minimal_skeleton = file("${path.module}/../../build/snippets/cloud_init_minimal_skeleton")
    storage_device         = hcloud_volume.test_x86.linux_device
    hetzner_dns_api_token  = var.hetzner_dns_api_token
    ssl_domain             = "test1.blcks.de"
  })
}

module "test_arm" {
  source      = "../../../testbeds/hetzner/test-server"
  name        = "test-arm"
  server_type = "cax11"
  user_data   = templatefile("${path.module}/cloud_init.sh", {
    solidblocks_base_url   = "https://${module.bootstrap_bucket.bucket_domain_name}"
    cloud_minimal_skeleton = file("${path.module}/../../build/snippets/cloud_init_minimal_skeleton")
    storage_device         = hcloud_volume.test_arm.linux_device
    hetzner_dns_api_token  = var.hetzner_dns_api_token
    ssl_domain             = "test2.blcks.de"
  })
}

resource "aws_s3_object" "bootstrap" {
  bucket = module.bootstrap_bucket.bucket_id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  source = "${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  etag   = filemd5("${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip")
}