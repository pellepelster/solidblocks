resource "tls_private_key" "ssh_client_identity" {
  algorithm = "RSA"
}

resource "hcloud_ssh_key" "root" {
  name       = "${var.environment}-${var.name}-root"
  public_key = tls_private_key.ssh_client_identity.public_key_openssh
  labels     = local.default_labels
}

resource "tls_private_key" "ssh_host_identity" {
  algorithm = "RSA"
}
