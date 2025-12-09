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
}

module "s3_docker" {
  source = "../../../modules/s3-docker"
  name   = "s3-docker-1"

  ssh_keys = [data.hcloud_ssh_key.ssh_key.id]

  s3_buckets = [
    { name                     = var.bucket1_name,
      enable_public_web_access = true,
    },
    { name             = var.bucket2_name,
      owner_key_id     = random_bytes.bucket2_owner_key_id.hex,
      owner_secret_key = random_bytes.bucket2_owner_secret_key.hex
    },
  ]

  docker_users = [{ username : "yolo", password : "yolo" }]

  dns_zone = "blcks.de"
}
