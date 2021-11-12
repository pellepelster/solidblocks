terraform {
  backend "local" {
    workspace_dir = "../../../terraform-state/vault-instances/"
  }

  required_providers {
    hetznerdns = {
      source  = "timohirt/hetznerdns"
      version = "1.1.1"
    }
    hcloud     = {
      source  = "hetznercloud/hcloud"
      version = "1.32.0"
    }
    template   = {
      source  = "hashicorp/template"
      version = "2.2.0"
    }
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

provider "hetznerdns" {
  apitoken = var.hetznerdns_token
}

resource "hcloud_server" "vault-1" {
  name        = "vault-1"
  image       = "debian-9"
  server_type = "cx11"
  location    = var.location_1
  user_data   = data.template_file.user_data.rendered
  ssh_keys    = [data.hcloud_ssh_key.pelle.id]
}

resource "hcloud_floating_ip_assignment" "vault-1_ip_assignment" {
  floating_ip_id = hcloud_floating_ip.vault-1.id
  server_id      = hcloud_server.vault-1.id
}

resource "hcloud_floating_ip" "vault-1" {
  name          = "vault-1"
  type          = "ipv4"
  home_location = var.location_1
}

resource "hcloud_volume_attachment" "vault-1_volume_attachment" {
  volume_id = data.hcloud_volume.vault-1_storage.id
  server_id = hcloud_server.vault-1.id
}

resource "hetznerdns_record" "vault_1" {
  zone_id = data.hetznerdns_zone.dns_zone.id
  name    = "vault-1.${var.environment}"
  value   = hcloud_floating_ip.vault-1.ip_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "vault" {
  zone_id = data.hetznerdns_zone.dns_zone.id
  name    = "vault.${var.environment}"
  value   = hcloud_floating_ip.vault-1.ip_address
  type    = "A"
  ttl     = 60
}
