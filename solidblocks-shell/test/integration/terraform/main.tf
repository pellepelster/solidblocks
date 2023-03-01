terraform {
  backend "s3" {
    key = "my-project/terraform.state"
  }
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.56.0"
    }
  }
}

provider "aws" {
}

resource "random_uuid" "test_id" {}

resource "aws_s3_bucket" "bucket" {
  bucket = "test-${random_uuid.test_id.id}"
}