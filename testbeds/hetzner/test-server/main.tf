resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = var.name
  public_key = tls_private_key.ssh_key.public_key_openssh
}

module "ssh-config" {
  source              = "../ssh-config"
  ipv4_address        = hcloud_server.server.ipv4_address
  private_key_openssh = tls_private_key.ssh_key.private_key_openssh
  public_key_openssh  = tls_private_key.ssh_key.public_key_openssh
}

resource "hcloud_server" "server" {
  name        = var.name
  image       = "debian-11"
  server_type = var.server_type
  ssh_keys    = [hcloud_ssh_key.ssh_key.id]
  location    = var.location
  user_data   = var.user_data
}
