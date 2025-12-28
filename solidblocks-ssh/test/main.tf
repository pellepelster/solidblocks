module "openssh_ca" {
  source = "../modules/openssh-ca"
}

module "host1" {
  source      = "../modules/openssh-host-identity"
  ca_key_pem  = module.openssh_ca.ca_key_pem
  ca_cert_pem = module.openssh_ca.ca_cert_pem
}


output "host_certificate_openssh" {
  value = module.host1.host_certificate_openssh
}

output "host_key_openssh" {
  sensitive = true
  value     = module.host1.host_key_openssh
}
