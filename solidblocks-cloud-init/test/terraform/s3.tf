resource "aws_s3_bucket" "bootstrap" {
  bucket        = "test-${random_string.test_id.id}"
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
    sid = "AllowEveryoneReadOnlyAccess"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    principals {
      identifiers = ["*"]
      type        = "*"
    }
    resources = [
      "arn:aws:s3:::test-${random_string.test_id.id}",
      "arn:aws:s3:::test-${random_string.test_id.id}/*"
    ]
  }
}

resource "aws_s3_bucket_policy" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id
  policy = data.aws_iam_policy_document.bootstrap.json
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
