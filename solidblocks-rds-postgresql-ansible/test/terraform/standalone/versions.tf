terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "1.51.0"
    }
  }
}

terraform {
  backend "s3" {
    region = "eu-central-1"
    bucket = "solidblocks-test"
    key    = "test-solidblocks-rds-postgresql-ansible-standalone"
  }
}
