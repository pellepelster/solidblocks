variable "solidblocks_version" {
}

variable "location" {
  type    = string
  default = "hel1"
}

variable "test_id" {
  type = string
}

variable "restore_pitr" {
  type    = string
  default = null
}

variable "hetzner_s3_access_key" {
  type = string
}

variable "hetzner_s3_secret_key" {
  type = string
}