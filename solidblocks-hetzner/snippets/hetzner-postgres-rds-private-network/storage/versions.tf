terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.48.0"
    }
  }
}

provider "hcloud" {}
