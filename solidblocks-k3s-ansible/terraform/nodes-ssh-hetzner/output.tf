output "root_ssh_key_id" {
  value       = hcloud_ssh_key.root.id
  description = "Hetzner id of the created SSH key"
}

output "ssh_private_key_openssh" {
  value       = tls_private_key.ssh_client_identity.private_key_openssh
  description = "private part of the SSH key in OpenSSH format"
}

output "root_ssh_key_openssh_public" {
  value       = tls_private_key.ssh_client_identity.public_key_openssh
  description = "public part of the SSH key in OpenSSH format"
}