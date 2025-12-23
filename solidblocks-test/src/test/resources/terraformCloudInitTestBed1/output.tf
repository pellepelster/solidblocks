output "private_key_openssh_ed25519" {
  value     = tls_private_key.ssh_key_ed25519.private_key_openssh
  sensitive = true
}

output "private_key_pem_ed25519" {
  value     = tls_private_key.ssh_key_ed25519.private_key_pem
  sensitive = true
}

output "private_key_openssh_rsa" {
  value     = tls_private_key.ssh_key_rsa.private_key_openssh
  sensitive = true
}

output "private_key_pem_rsa" {
  value     = tls_private_key.ssh_key_rsa.private_key_pem
  sensitive = true
}

output "private_key_openssh_ecdsa" {
  value     = tls_private_key.ssh_key_ecdsa.private_key_openssh
  sensitive = true
}

output "private_key_pem_ecdsa" {
  value     = tls_private_key.ssh_key_ecdsa.private_key_pem
  sensitive = true
}

output "ipv4_address" {
  value     = hcloud_server.server.ipv4_address
  sensitive = true
}

