terraform {
  backend "local" {
    workspace_dir = "../../../terraform-state/backup-instances/"
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
    vault      = {
      source  = "hashicorp/vault"
      version = "2.24.1"
    }
  }
}

provider "vault" {
  address = "https://vault.${var.environment}.${var.root_domain}:8200"
  token   = var.vault_token
}

provider "hcloud" {
  token = var.hcloud_token
}

provider "hetznerdns" {
  apitoken = var.hetznerdns_token
}

resource "hcloud_server" "backup" {
  name        = "backup"
  image       = "debian-9"
  server_type = "cx21"
  location    = var.location_1
  user_data   = data.template_file.user_data.rendered
  ssh_keys    = [data.hcloud_ssh_key.pelle.id]
}

resource "hcloud_floating_ip_assignment" "backup_ip_assignment" {
  floating_ip_id = hcloud_floating_ip.backup.id
  server_id      = hcloud_server.backup.id
}

resource "hcloud_floating_ip" "backup" {
  name          = "backup_"
  type          = "ipv4"
  home_location = var.location_1
}

resource "hcloud_volume_attachment" "backup__volume_attachment" {
  volume_id = data.hcloud_volume.backup_storage.id
  server_id = hcloud_server.backup.id
}

resource "hetznerdns_record" "backup" {
  zone_id = data.hetznerdns_zone.dns_zone.id
  name    = "backup.${var.environment}"
  value   = hcloud_floating_ip.backup.ip_address
  type    = "A"
  ttl     = 60
}

resource "vault_token" "cloud_init" {
  policies = ["cloud_init"]
}