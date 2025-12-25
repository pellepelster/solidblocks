output "ipv4_address" {
  value = module.web-s3-docker.ipv4_address
}

output "private_key" {
  value     = tls_private_key.ssh_key.private_key_pem
  sensitive = true
}

