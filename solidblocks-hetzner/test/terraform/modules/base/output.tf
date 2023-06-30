output "ssh_key_id" {
  value = hcloud_ssh_key.ssh_key.id
}

output "bootstrap_bucket_domain_name" {
  value = aws_s3_bucket.bootstrap.bucket_domain_name
}