terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.48.0"
    }
    http = {
      source  = "hashicorp/http"
      version = ">= 3.3.0"
    }
  }
}
