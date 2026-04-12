resource "github_actions_secret" "aws_access_key_id" {
  repository       = "solidblocks"
  secret_name      = "AWS_ACCESS_KEY_ID"
  plaintext_value  = aws_iam_access_key.solidblocks_test.id
}

resource "github_actions_secret" "aws_secret_access_key" {
  repository       = "solidblocks"
  secret_name      = "AWS_SECRET_ACCESS_KEY"
  plaintext_value  = aws_iam_access_key.solidblocks_test.secret
}