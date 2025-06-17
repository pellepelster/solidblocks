output "backup_s3_bucket" {
  value = scaleway_object_bucket.backup.name
}

output "backup_s3_endpoint" {
  value = scaleway_object_bucket.backup.api_endpoint
}

output "backup_s3_key" {
  value     = scaleway_iam_api_key.backup.access_key
  sensitive = true
}

output "backup_s3_key_secret" {
  value     = scaleway_iam_api_key.backup.secret_key
  sensitive = true
}

output "backup_s3_region" {
  value = scaleway_object_bucket.backup.region
}
