variable "hetzner_location" {
  default = "nbg1"
}

variable "aws_region" {
  default = "eu-central-1"
}

variable "backup_s3_access_key" {
  type = string
}

variable "backup_s3_secret_key" {
  type = string
}
