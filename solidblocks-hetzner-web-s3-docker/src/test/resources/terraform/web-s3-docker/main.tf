data "hcloud_ssh_key" "ssh_key" {
  name = "test-${var.test_id}"
}

resource "random_bytes" "bucket2_owner_key_id" {
  length = 12
}

resource "random_bytes" "bucket2_owner_secret_key" {
  length = 32
}

locals {
  docker_user     = "yolo1"
  docker_password = "yolo2"
  name            = "web-s3-docker"
}

module "web_s3_docker" {
  source = "../../../../../modules/web-s3-docker"
  name   = local.name

  dns_zone             = "blcks-test.de"
  ssh_keys             = [data.hcloud_ssh_key.ssh_key.id]
  docker_public_enable = var.docker_public_enable

  s3_buckets = [
    { name                     = var.bucket1_name,
      web_access_public_enable = true,
      web_access_domains       = ["blcks-test.de", "www.blcks-test.de", "www.${local.name}.blcks-test.de"]
    },
    { name             = var.bucket2_name,
      owner_key_id     = random_bytes.bucket2_owner_key_id.hex,
      owner_secret_key = random_bytes.bucket2_owner_secret_key.hex
    },
  ]
}
