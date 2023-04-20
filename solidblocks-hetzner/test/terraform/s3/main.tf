resource "aws_s3_bucket" "bootstrap" {
  bucket        = "test-${var.test_id}"
  force_destroy = true
}

locals {
  base_path         = "${path.module}/../../../../solidblocks-cloud-init/"
  bootstrap_zip     = tolist(fileset(local.base_path, "**/solidblocks-cloud-init-${var.solidblocks_version}.zip"))[0]
  bootstrap_snippet = tolist(fileset(local.base_path, "**/cloud_init_bootstrap_solidblocks"))[0]
}

resource "aws_s3_object" "bootstrap_zip" {
  bucket = aws_s3_bucket.bootstrap.id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  source = "${local.base_path}/${local.bootstrap_zip}"
  acl    = "public-read"
  etag   = filemd5("${local.base_path}/${local.bootstrap_zip}")
}

resource "aws_s3_object" "bootstrap_snippet" {
  bucket = aws_s3_bucket.bootstrap.id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/cloud_init_bootstrap_solidblocks"
  source = "${local.base_path}/${local.bootstrap_snippet}"
  acl    = "public-read"
  etag   = filemd5("${local.base_path}/${local.bootstrap_snippet}")
}

