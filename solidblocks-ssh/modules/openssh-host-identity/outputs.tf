output "host_key_openssh" {
  value     = tls_private_key.ssh_host_private_key.private_key_openssh
  sensitive = true
}

output "host_certificate_openssh" {
  value = tls_locally_signed_cert.ssh_host_certificate.cert_pem
}
