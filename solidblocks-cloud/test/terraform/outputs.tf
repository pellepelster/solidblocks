output "test_ip" {
  value = hcloud_server.test.ipv4_address
}

output "bootstrap" {
  value = "https://${aws_s3_bucket.bootstrap.bucket_domain_name}/${aws_s3_object.bootstrap.key}"
}