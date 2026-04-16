terraform {
  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "2.5.2"
    }
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.38.2"
    }
  }
}

provider "hcloud" {}
