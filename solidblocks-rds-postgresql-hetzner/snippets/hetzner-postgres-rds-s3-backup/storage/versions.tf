terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.48.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

provider "hcloud" {}
