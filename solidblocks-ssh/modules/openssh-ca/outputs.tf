output "ca_key_pem" {
  value = tls_private_key.ssh_ca_key.private_key_pem
}

output "ca_cert_pem" {
  value = tls_self_signed_cert.ssa_ca_cert.cert_pem
}
