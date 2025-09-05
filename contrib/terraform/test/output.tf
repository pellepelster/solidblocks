output "test_access_key_id" {
  value = aws_iam_access_key.solidblocks_test.id
}

output "test_access_key_secret" {
  value     = aws_iam_access_key.solidblocks_test.secret
  sensitive = true
}
