terraform {
  required_providers {
    tls = {
      source  = "hashicorp/tls"
      version = "4.0.1"
    }
  }
}

# sshd host key
resource tls_private_key sshd_host_ecdsa_key {
  algorithm   = "ECDSA"
  ecdsa_curve = "P384"
}

resource local_file sshd_host_ecdsa_public {
  filename = "sshd_host_ecdsa_public"
  content  = tls_private_key.sshd_host_ecdsa_key.public_key_openssh
}

resource local_file sshd_host_ecdsa_key {
  filename = "sshd_host_ecdsa_key"
  content  = tls_private_key.sshd_host_ecdsa_key.private_key_pem
}

# postgresql ssh key
resource tls_private_key postgresql_ssh_key {
  algorithm   = "ECDSA"
  ecdsa_curve = "P384"
}

resource local_file postgresql_ssh_public {
  filename = "postgresql_ssh_public"
  content  = tls_private_key.postgresql_ssh_key.public_key_openssh
}

resource local_file postgresql_ssh_private {
  filename = "postgresql_ssh_private"
  content  = tls_private_key.postgresql_ssh_key.private_key_pem
}

