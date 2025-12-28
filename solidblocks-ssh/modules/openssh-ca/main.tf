resource "tls_private_key" "ssh_ca_key" {
  algorithm = "ED25519"
}

resource "tls_self_signed_cert" "ssa_ca_cert" {
  private_key_pem = tls_private_key.ssh_ca_key.private_key_pem

  is_ca_certificate = true

  subject {
    country             = "IN"
    province            = "Mahrashatra"
    locality            = "Mumbai"
    common_name         = "Cloud Manthan Root CA"
    organization        = "Cloud Manthan Software Solutions Pvt Ltd."
    organizational_unit = "Cloud Manthan Root Certification Auhtority"
  }

  validity_period_hours = 43800
  allowed_uses = [
    "digital_signature",
    "cert_signing",
    "crl_signing",
  ]
}
