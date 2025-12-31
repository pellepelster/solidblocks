resource "aws_iam_user" "solidblocks_test" {
  name = "test-blcks"
}

resource "aws_iam_access_key" "solidblocks_test" {
  user = aws_iam_user.solidblocks_test.name
}

data "aws_iam_policy_document" "solidblocks_test_s3" {
  statement {
    effect    = "Allow"
    actions   = ["s3:*"]
    resources = ["arn:aws:s3:::test-*/**"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:ListAllMyBuckets"]
    resources = ["*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:*"]
    resources = ["arn:aws:s3:::test-*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::test-*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::test-*/**"]
  }
}

data "aws_iam_policy_document" "solidblocks_test_dynamodb" {
  statement {
    effect    = "Allow"
    actions   = ["dynamodb:*"]
    resources = ["arn:aws:dynamodb:*:*:table/test-*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["dynamodb:List*"]
    resources = ["arn:aws:dynamodb:*:*:table/*"]
  }
  statement {
    effect = "Allow"
    actions = ["dynamodb:List*",
      "dynamodb:Describe*"
    ]
    resources = ["arn:aws:dynamodb:*:*:table/"]
  }
}

resource "aws_iam_user_policy" "solidblocks" {
  name   = "test-blcks"
  user   = aws_iam_user.solidblocks_test.name
  policy = data.aws_iam_policy_document.solidblocks_test_s3.json
}


resource "aws_s3_bucket" "bootstrap" {
  bucket = "test-blcks-bootstrap"
}

resource "aws_s3_bucket_public_access_block" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "bootstrap" {
  bucket = aws_s3_bucket.bootstrap.id

  policy = jsonencode({
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.bootstrap.arn}/*"
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.bootstrap]
}
