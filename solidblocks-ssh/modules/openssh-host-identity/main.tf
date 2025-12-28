resource "tls_private_key" "ssh_host_private_key" {
  algorithm = "ED25519"
}

resource "tls_cert_request" "ssh_host_csr" {

  private_key_pem = tls_private_key.ssh_host_private_key.private_key_pem

  dns_names = ["dev.cloudmanthan.internal"]

  subject {
    country             = "IN"
    province            = "Mahrashatra"
    locality            = "Mumbai"
    common_name         = "Cloud Manthan Internal Development "
    organization        = "Cloud Manthan"
    organizational_unit = "Development"
  }
}

resource "tls_locally_signed_cert" "ssh_host_certificate" {
  cert_request_pem = tls_cert_request.ssh_host_csr.cert_request_pem

  ca_private_key_pem = var.ca_key_pem
  ca_cert_pem        = var.ca_cert_pem

  validity_period_hours = 43800

  allowed_uses = [
    "digital_signature",
    "key_encipherment",
    "server_auth",
    "client_auth",
  ]
}
