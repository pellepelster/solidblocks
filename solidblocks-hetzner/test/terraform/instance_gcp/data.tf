data "aws_s3_bucket" "bootstrap" {
  bucket = "test-${var.test_id}"
}

data "google_storage_bucket" "backup" {
  name = "test-${var.test_id}"
}

data "hcloud_ssh_key" "ssh_key" {
  name = "test-${var.test_id}"
}
