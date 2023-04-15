terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.38.0"
    }
    http = {
      source  = "hashicorp/http"
      version = "3.2.1"
    }
  }
}
