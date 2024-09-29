terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.48.0"
    }
    minio = {
      source = "aminueza/minio"
    }
  }
}

provider "hcloud" {}
