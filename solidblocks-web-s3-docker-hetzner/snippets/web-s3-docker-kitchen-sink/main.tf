resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key1" {
  name       = "ssh-key1"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

module "web-s3-docker" {
  source   = "https://github.com/pellepelster/solidblocks/releases/download/v0.4.11/terraform-hcloud-blcks-web-s3-docker-v0.4.11.zip"
  name     = "server1"
  dns_zone = "blcks-test.de"

  ssh_keys = [hcloud_ssh_key.ssh_key1.id]

  s3_buckets = [
    {
      name                     = "bucket1",
      web_access_public_enable = true,
      web_access_domains       = ["blcks-test.de", "www.blcks-test.de"]
    },
    {
      name             = "bucket2",
      owner_key_id     = "cbeebb7f1fa4de50025a5c95",
      owner_secret_key = "f775d527e821ab035b5a874e6326f20c135b2d2fc903112fe768a17681f54043"
    },
  ]

  disable_volume_delete_protection = true
}

