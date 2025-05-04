resource "local_file" "ssh_client_identity" {
  filename        = "${var.output_path}/ssh/client_identity"
  content         = var.ssh_private_key_openssh
  file_permission = "0600"
}

resource "local_file" "ssh_config" {
  filename = "${var.output_path}/ssh/client_config"
  content  = templatefile("${path.module}/templates/ssh_config.template", {
    servers              = var.ssh_servers
    client_identity_file = abspath(local_file.ssh_client_identity.filename)
  }
  )
}
