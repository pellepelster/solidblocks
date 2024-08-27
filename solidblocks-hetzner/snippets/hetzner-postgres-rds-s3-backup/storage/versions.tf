terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.56.0"
    }
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
