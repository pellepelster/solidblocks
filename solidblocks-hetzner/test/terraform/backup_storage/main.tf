resource "hcloud_volume" "backup" {
  name     = "test-backup-${var.test_id}"
  size     = 32
  format   = "ext4"
  location = var.location
}