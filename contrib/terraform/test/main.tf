resource "aws_iam_user" "solidblocks_test" {
  name = "solidblocks-test"
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
    resources = ["arn:aws:s3:::solidblocks-test"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::solidblocks-test/**"]
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
  name   = "solidblocks-test"
  user   = aws_iam_user.solidblocks_test.name
  policy = data.aws_iam_policy_document.solidblocks_test_s3.json
}
