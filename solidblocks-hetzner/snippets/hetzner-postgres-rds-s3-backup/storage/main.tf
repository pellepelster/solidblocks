resource "aws_s3_bucket" "backup" {
  bucket        = "test-rds-postgresql-backup"
  force_destroy = true
}

resource hcloud_volume "data" {
  name     = "rds-postgresql-data"
  size     = 32
  format   = "ext4"
  location = var.hetzner_location
}

