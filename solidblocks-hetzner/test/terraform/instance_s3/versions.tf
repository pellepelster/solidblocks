terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.56.0"
    }
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.38.0"
    }
  }
}

provider "aws" {
  region = "eu-central-1"
}

provider hcloud {}
