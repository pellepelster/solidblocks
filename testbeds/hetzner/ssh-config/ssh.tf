resource "local_file" "ssh_private_key" {
  content         = var.private_key_openssh
  filename        = "${path.root}/id_rsa"
  file_permission = "0600"
}

resource "local_file" "ssh_public_key" {
  content         = var.public_key_openssh
  filename        = "${path.root}/id_rsa.pub"
  file_permission = "0600"
}

resource "local_file" "ssh_public_config" {
  content = templatefile("${path.module}/ssh_config.template", {
    "ipv4_address"  = var.ipv4_address
    "identity_file" = abspath(local_file.ssh_private_key.filename)
  }
  )
  filename        = "${abspath(path.root)}/ssh_config"
  file_permission = "0600"
}

