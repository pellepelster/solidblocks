resource "random_string" "test_id" {
  length  = 8
  upper   = false
  lower   = true
  special = false
}

resource "tls_private_key" "ssh_key_ed25519" {
  algorithm = "ED25519"
}

resource "tls_private_key" "ssh_key_rsa" {
  algorithm = "RSA"
}

resource "tls_private_key" "ssh_key_ecdsa" {
  algorithm = "ECDSA"
}

resource "hcloud_ssh_key" "ssh_key_ed25519" {
  name       = "${random_string.test_id.id}-ed25519"
  public_key = tls_private_key.ssh_key_ed25519.public_key_openssh
}

resource "hcloud_ssh_key" "ssh_key_rsa" {
  name       = "${random_string.test_id.id}-rsa"
  public_key = tls_private_key.ssh_key_rsa.public_key_openssh
}

resource "hcloud_server" "server" {
  name        = random_string.test_id.id
  image       = "debian-12"
  server_type = "cx23"
  ssh_keys    = [hcloud_ssh_key.ssh_key_ed25519.id, hcloud_ssh_key.ssh_key_rsa.id]
}
