terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

terraform {
  backend "s3" {
    region         = "eu-central-1"
    bucket         = "test-blcks-terraform"
    key            = "test"
  }
}

provider "aws" {
  region = "eu-central-1"
}
