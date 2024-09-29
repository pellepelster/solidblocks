resource "hcloud_volume" "data" {
  name     = "rds-postgresql-data"
  size     = 32
  format   = "ext4"
  location = var.hetzner_location
}

resource "minio_s3_bucket" "backup" {
  bucket = "rds-postgresql-backup"
  acl    = "private"
}
