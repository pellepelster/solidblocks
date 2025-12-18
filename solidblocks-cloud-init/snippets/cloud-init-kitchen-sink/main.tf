resource "hcloud_volume" "example" {
  name     = "example"
  size     = 16
  format   = "ext4"
  location = var.location
}

resource "hcloud_volume_attachment" "example" {
  server_id = hcloud_server.example.id
  volume_id = hcloud_volume.example.id
}

module "example" {
  source  = "github.com/pellepelster/terraform-null-blcks-cloud-init?ref=v0.4.8-rc1"

  storage = [
    { linux_device = hcloud_volume.example.linux_device, mount_path = "/data1" },
  ]

  acme_ssl = {
    path         = "/data1/ssl"
    email        = "contact@blcks.de"
    domains      = ["example.blcks.de"]
    acme_server  = "https://acme-staging-v02.api.letsencrypt.org/directory"
    dns_provider = "hetzner"
    variables = {
      HETZNER_API_KEY : var.hetzner_dns_api_token
      HETZNER_HTTP_TIMEOUT : "30"
      HETZNER_PROPAGATION_TIMEOUT : "300"
    }
  }
}

resource "hcloud_server" "example" {
  name        = "test"
  image       = "debian-11"
  server_type = "cx23"
  location    = var.location
  ssh_keys    = [hcloud_ssh_key.ssh_key.id]
  user_data   = <<EOT
${module.example.user_data}

# do something with the generated certificate
ls -lsa /data1/ssl/certificates/example.blcks.de.crt
EOT
}
