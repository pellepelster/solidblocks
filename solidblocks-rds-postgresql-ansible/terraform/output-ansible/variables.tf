variable "name" {
  type        = string
  description = "Name for the K3S cluster associated resources"
}

variable "environment" {
  type        = string
  description = "Environment/stage for the resources (prod, dev, staging)"
}

variable "output_path" {
  type        = string
  description = "path for the generated files"
}

variable "ssh_config_file" {
  type        = string
  description = "path to the ssh client config file for K3S node access"
}

variable "backup_s3_endpoint" {
  type = string
}
variable "backup_s3_bucket" {
  type = string
}

variable "backup_s3_key" {
  type = string
}

variable "backup_s3_key_secret" {
  type = string
}

variable "backup_s3_region" {
  type = string
}

variable "backup_s3_uri_style" {
  type    = string
  default = "host"
}

variable "databases" {
  type = map(object({
    servers = list(object({ name = string, ipv4_address = string, data_linux_device = string }))
  }))
}
