terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">= 1.38.2"
    }
    http = {
      source  = "hashicorp/http"
      version = ">= 3.3.0"
    }
  }
}
