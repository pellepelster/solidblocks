variable "solidblocks_rds_version" {
}

variable "location" {
  type    = string
  default = "hel1"
}

variable "test_id" {
  type = string
}

variable "backup_s3_access_key" {
  type = string
}

variable "backup_s3_secret_key" {
  type = string
}

variable "hetzner_api_token" {
  type = string
}

