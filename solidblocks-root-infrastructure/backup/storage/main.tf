terraform {
  backend "local" {
    workspace_dir = "../../../terraform-state/backup-storage/"
  }
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.32.0"
    }
  }
}

provider "hcloud" {
  token = var.hcloud_token
}

resource "hcloud_volume" "backup_storage_fsn1" {
  name     = "backup_storage_fsn1"
  size     = 64
  format   = "ext4"
  location = "fsn1"
}