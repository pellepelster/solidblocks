resource "random_string" "test_id" {
  length  = 16
  special = false
  lower   = true
  upper   = false
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "test-${random_string.test_id.id}"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource hcloud_volume "test" {
  name     = "test-${random_string.test_id.id}"
  size     = 32
  format   = "ext4"
  location = var.location
}

resource hcloud_volume_attachment test {
  server_id = hcloud_server.test.id
  volume_id = hcloud_volume.test.id
}

resource hcloud_server "test" {
  name        = "test-${random_string.test_id.id}"
  image       = "debian-11"
  server_type = "cx11"
  ssh_keys    = [hcloud_ssh_key.ssh_key.id]
  location    = var.location
  user_data   = templatefile("${path.module}/cloud_init.sh", {
    solidblocks_base_url   = "https://${aws_s3_bucket.bootstrap.bucket_domain_name}"
    cloud_minimal_skeleton = file("${path.module}/../../build/snippets/cloud_init_minimal_skeleton")
    storage_device         = hcloud_volume.test.linux_device
  })
}

resource "aws_s3_bucket" "bootstrap" {
  bucket = "test-${random_string.test_id.id}"
}

resource "aws_s3_object" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  source = "${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  acl    = "public-read"
  etag   = filemd5("${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip")
}