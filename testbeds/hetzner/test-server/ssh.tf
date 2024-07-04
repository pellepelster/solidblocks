resource "local_file" "ssh_private_key" {
  content         = tls_private_key.ssh_key.private_key_openssh
  filename        = "${path.module}/${var.name}_id_rsa"
  file_permission = "0600"
}

resource "local_file" "ssh_public_key" {
  content         = tls_private_key.ssh_key.public_key_openssh
  filename        = "${path.module}/${var.name}_id_rsa.pub"
  file_permission = "0600"
}

resource "local_file" "ssh_public_config" {
  content = templatefile("${path.module}/ssh_config.template", {
    "hostname"      = hcloud_server.server.ipv4_address
    "identity_file" = abspath(local_file.ssh_private_key.filename)
  }
  )
  filename        = "${abspath(path.module)}/${var.name}_ssh_config"
  file_permission = "0600"
}

