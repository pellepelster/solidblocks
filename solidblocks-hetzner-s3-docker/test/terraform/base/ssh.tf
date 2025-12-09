resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "hcloud_ssh_key" "ssh_key" {
  name       = "test-${random_string.test_id.id}"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource "local_file" "ssh_private_key" {
  content         = tls_private_key.ssh_key.private_key_openssh
  filename        = "${path.module}/../test_id_rsa"
  file_permission = "0600"
}

resource "local_file" "ssh_public_key" {
  content         = tls_private_key.ssh_key.public_key_openssh
  filename        = "${path.module}/../test_id_rsa.pub"
  file_permission = "0600"
}

resource "local_file" "ssh_public_config" {
  content = templatefile("${path.module}/ssh_config.template", {
    "identity_file" = abspath(local_file.ssh_private_key.filename)
    }
  )
  filename        = "${path.module}/../ssh_config"
  file_permission = "0600"
}
