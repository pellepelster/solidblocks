terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">=4.50"
    }
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.38.2"
    }
  }
}

