terraform {
  required_providers {
    scaleway = {
      source  = "scaleway/scaleway"
      version = "2.55.0"
    }
  }
}

terraform {
  backend "s3" {
    region = "eu-central-1"
    bucket = "solidblocks-test"
    key    = "test-solidblocks-rds-postgresql-ansible-backup"
  }
}
