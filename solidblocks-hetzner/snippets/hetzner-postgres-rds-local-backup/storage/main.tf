resource hcloud_volume "data" {
  name     = "rds-postgresql-data"
  size     = 32
  format   = "ext4"
  location = var.hetzner_location
}

resource hcloud_volume "backup" {
  name     = "rds-postgresql-backup"
  size     = 32
  format   = "ext4"
  location = var.hetzner_location
}
