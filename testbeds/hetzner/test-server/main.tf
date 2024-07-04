resource "random_string" "random" {
  length  = 5
  special = false
  upper   = false
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "test-${random_string.random.id}"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource "hcloud_server" "server" {
  name        = "${var.name}-${random_string.random.id}"
  image       = "debian-11"
  server_type = var.server_type
  ssh_keys    = [hcloud_ssh_key.ssh_key.id]
  location    = var.location
  user_data   = var.user_data
}
