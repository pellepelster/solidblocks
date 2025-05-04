output "root_ssh_key_id" {
  value = hcloud_ssh_key.root.id
}

output "ssh_private_key_openssh" {
  value = tls_private_key.ssh_client_identity.private_key_openssh
}

output "root_ssh_key_openssh_public" {
  value = tls_private_key.ssh_client_identity.public_key_openssh
}