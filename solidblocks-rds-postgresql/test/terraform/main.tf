terraform {
  required_providers {
    tls = {
      source  = "hashicorp/tls"
      version = "4.0.1"
    }
  }
}

resource "tls_private_key" "ca" {
  algorithm   = "ECDSA"
  ecdsa_curve = "P384"
}

resource "tls_self_signed_cert" "ca" {
  private_key_pem = tls_private_key.ca.private_key_pem

  subject {
    common_name  = "SolidBlocks Test CA"
    organization = "SolidBlocks"
  }

  is_ca_certificate     = true
  validity_period_hours = 8766
  early_renewal_hours   = 24

  allowed_uses = [
    "key_encipherment",
    "digital_signature",
    "server_auth",
    "client_auth",
    "cert_signing",
    "crl_signing",
    "any_extended",
  ]
}

resource "local_file" "kube_ca_crt" {
  content  = tls_self_signed_cert.ca.cert_pem
  filename = "../src/test/resources/ca.pem"
}


resource "local_file" "ca_key" {
  content  = tls_private_key.ca.private_key_pem
  filename = "./certificates/ca.key.pem"
}
