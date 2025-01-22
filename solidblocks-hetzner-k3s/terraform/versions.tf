terraform {
  required_providers {
    hetznerdns = {
      source  = "timohirt/hetznerdns"
      version = "2.2.0"
    }
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.49"
    }
  }
}

provider "hcloud" {
}

provider "hetznerdns" {
}