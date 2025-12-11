resource "random_bytes" "rpc_secret" {
  length = 32
}

resource "random_bytes" "admin_token" {
  length = 32
}

resource "random_bytes" "metrics_token" {
  length = 32
}

resource "random_bytes" "owner_secret_keys" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 32
}

resource "random_bytes" "owner_key_ids" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 12
}

resource "random_bytes" "ro_secret_keys" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 32
}

resource "random_bytes" "ro_key_ids" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 12
}

resource "random_bytes" "rw_secret_keys" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 32
}

resource "random_bytes" "rw_key_ids" {
  for_each = { for s3_bucket in var.s3_buckets : s3_bucket.name => s3_bucket.name }
  length   = 12
}
