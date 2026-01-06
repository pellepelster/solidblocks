output "test_id" {
  value = random_string.test_id.id
}

output "ssh_key_name" {
  value = hcloud_ssh_key.ssh_key.name
}
