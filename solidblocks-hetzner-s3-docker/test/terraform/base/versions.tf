terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.48.0"
    }

  }
}

provider "aws" {
  region = "eu-central-1"
}

provider "hcloud" {}
