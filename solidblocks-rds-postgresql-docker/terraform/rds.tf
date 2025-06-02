resource "tls_private_key" "rds" {
  algorithm   = "ECDSA"
  ecdsa_curve = "P384"
}

resource "tls_cert_request" "rds" {
  private_key_pem = tls_private_key.rds.private_key_pem

  dns_names    = ["rds", "localhost"]
  ip_addresses = ["172.17.0.2"]

  subject {
    common_name  = "rds"
    organization = "Solidblocks"
  }
}

resource "tls_locally_signed_cert" "rds" {
  cert_request_pem = tls_cert_request.rds.cert_request_pem

  ca_private_key_pem = tls_private_key.ca.private_key_pem
  ca_cert_pem        = tls_self_signed_cert.ca.cert_pem

  validity_period_hours = 8760

  allowed_uses = [
    "client_auth",
    "server_auth",
  ]
}

resource "local_file" "rds_key" {
  content  = tls_private_key.rds.private_key_pem
  filename = "../src/test/resources/rds.key.pem"
}

resource "local_file" "rds_crt" {
  content  = tls_locally_signed_cert.rds.cert_pem
  filename = "../src/test/resources/rds.pem"
}
