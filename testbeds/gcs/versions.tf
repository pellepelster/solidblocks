terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "5.17.0"
    }
  }

  backend "gcs" {
    bucket = "solidblocks-test-terraform"
    prefix = "testbed"
  }
}

provider "google" {
}