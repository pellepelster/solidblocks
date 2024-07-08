locals {
  bootstrap_bucket_name = "test-bootstrap-${var.test_id}"
}

resource "aws_s3_bucket" "bootstrap" {
  bucket        = local.bootstrap_bucket_name
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

data "aws_iam_policy_document" "bootstrap" {
  statement {
    sid     = "AllowEveryoneReadOnlyAccess"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    principals {
      identifiers = ["*"]
      type        = "*"
    }
    resources = [
      "arn:aws:s3:::${local.bootstrap_bucket_name}",
      "arn:aws:s3:::${local.bootstrap_bucket_name}/*"
    ]
  }
}

resource "aws_s3_bucket_policy" "bootstrap" {
  bucket     = aws_s3_bucket.bootstrap.id
  policy     = data.aws_iam_policy_document.bootstrap.json
  depends_on = [
    aws_s3_bucket_ownership_controls.bootstrap, aws_s3_bucket_public_access_block.bootstrap, aws_s3_bucket_acl.bootstrap
  ]
}

resource "aws_s3_bucket_ownership_controls" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "bootstrap" {
  depends_on = [
    aws_s3_bucket_ownership_controls.bootstrap,
    aws_s3_bucket_public_access_block.bootstrap,
  ]

  bucket = aws_s3_bucket.bootstrap.id
  acl    = "public-read"
}


resource "aws_s3_object" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id
  key    = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  source = "${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip"
  etag   = filemd5("${path.module}/../../build/solidblocks-cloud-init-${var.solidblocks_version}.zip")
}

/*
resource "aws_s3_object" "bootstrap_snippet" {
  bucket     = aws_s3_bucket.bootstrap.id
  key        = "pellepelster/solidblocks/releases/download/${var.solidblocks_version}/cloud_init_bootstrap_solidblocks"
  source     = "${local.base_path}/${local.bootstrap_snippet}"
  etag       = filemd5("${local.base_path}/${local.bootstrap_snippet}")
  depends_on = [aws_s3_bucket_acl.bootstrap]
}
 */