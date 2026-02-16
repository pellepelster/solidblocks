output "test_id" {
  value = random_string.test_id.id
}

output "ssh_key_name" {
  value = hcloud_ssh_key.ssh_key.name
}

output "private_key" {
  value     = tls_private_key.ssh_key.private_key_pem
  sensitive = true
}
