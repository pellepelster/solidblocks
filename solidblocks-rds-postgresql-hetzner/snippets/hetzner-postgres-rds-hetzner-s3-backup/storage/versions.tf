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

provider "minio" {
  minio_server   = "${var.hetzner_location}.your-objectstorage.com"
  minio_user     = var.backup_s3_access_key
  minio_password = var.backup_s3_secret_keyy
  minio_region   = var.hetzner_location
  minio_ssl      = true
}
