data "aws_s3_bucket" "bootstrap" {
  bucket = "test-${var.test_id}"
}

data "hcloud_volume" "backup" {
  name = "test-backup-${var.test_id}"
}

data "hcloud_ssh_key" "ssh_key" {
  name = "test-${var.test_id}"
}

