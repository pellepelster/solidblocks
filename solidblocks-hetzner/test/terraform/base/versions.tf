terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.66.1"
    }
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = ">=1.48.0"
    }
    google = {
      source  = "hashicorp/google"
      version = "5.17.0"
    }
    minio = {
      source = "aminueza/minio"
    }
  }
}

provider "aws" {
  region = "eu-central-1"
}

provider "hcloud" {}

provider "google" {}

provider "minio" {
  minio_server   = "fsn1.your-objectstorage.com"
  minio_user     = var.hetzner_s3_access_key
  minio_password = var.hetzner_s3_secret_key
  minio_region   = "fsn1"
  minio_ssl      = true
}
