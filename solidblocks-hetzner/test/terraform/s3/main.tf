resource "aws_s3_bucket" "bootstrap" {
  bucket        = "test-${var.test_id}"
  force_destroy = true
}

resource "aws_s3_object" "bootstrap_zip" {
  bucket = aws_s3_bucket.bootstrap.id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  source = "${path.module}/../../../../solidblocks-cloud-init/build/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  acl    = "public-read"
  etag   = filemd5("${path.module}/../../../../solidblocks-cloud-init/build/solidblocks-cloud-init-${var.solidblocks_version}.zip")
}

resource "aws_s3_object" "bootstrap_snippet" {
  bucket = aws_s3_bucket.bootstrap.id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/cloud_init_bootstrap_solidblocks"
  source = "${path.module}/../../../../solidblocks-cloud-init/build/snippets/cloud_init_bootstrap_solidblocks"
  acl    = "public-read"
  etag   = filemd5("${path.module}/../../../../solidblocks-cloud-init/build/snippets/cloud_init_bootstrap_solidblocks")
}

