variable "id" {
  type        = string
  description = "unique name for the postgres rds instance"
}

variable "location" {
  type        = string
  description = "hetzner location"
}

variable "ssh_keys" {
  type        = list(number)
  description = "ssh keys for instance access"
}

variable "data_volume" {
  type        = number
  description = "data volume id"
}

variable "backup_volume" {
  type        = number
  description = "backup volume id"
}

variable "solidblocks_base_url" {
  type        = string
  default     = "https://github.com"
  description = "override base url for testing purposes"
}

variable "solidblocks_cloud_init_version" {
  type    = string
  default = "v0.0.84"
}

variable "solidblocks_version" {
  type    = string
  default = "v0.0.84"
}

variable "db_backup_s3_bucket" {
  type = string
}

variable "db_backup_s3_access_key" {
  type = string
}

variable "db_backup_s3_secret_key" {
  type = string
}
