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

resource "hcloud_volume" "test1" {
  name     = "test1-${random_string.test_id.id}"
  size     = 16
  format   = "ext4"
  location = local.location
}

resource "hcloud_volume_attachment" "test1" {
  server_id = module.test.server_id
  volume_id = hcloud_volume.test1.id
}

resource "hcloud_volume" "test2" {
  name     = "test2-${random_string.test_id.id}"
  size     = 16
  format   = "ext4"
  location = local.location
}

resource "hcloud_volume_attachment" "test2" {
  server_id = module.test.server_id
  volume_id = hcloud_volume.test2.id
}

module "cloud_init" {
  source               = "../../modules/solidblocks-cloud-init"
  solidblocks_base_url = "https://${module.bootstrap_bucket.bucket_domain_name}"
  storage              = [
    { linux_device = hcloud_volume.test1.linux_device, mount_path = "/data1" },
    { linux_device = hcloud_volume.test2.linux_device, mount_path = "/data2" }
  ]
}

module "test" {
  source      = "../../../testbeds/hetzner/test-server"
  name        = "test-${random_string.test_id.id}"
  server_type = "cx23"
  user_data   = module.cloud_init.user_data
}
