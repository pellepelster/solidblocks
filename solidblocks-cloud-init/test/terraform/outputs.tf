output "test_ip_x86" {
  value = hcloud_server.test-x86.ipv4_address
}

output "test_ip_arm" {
  value = hcloud_server.test-arm.ipv4_address
}

output "bootstrap" {
  value = "https://${aws_s3_bucket.bootstrap.bucket_domain_name}/${aws_s3_object.bootstrap.key}"
}