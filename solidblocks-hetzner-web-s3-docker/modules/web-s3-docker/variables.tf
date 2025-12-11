variable "name" {
  type        = string
  description = "Unique name for the server instance"
}

variable "location" {
  type        = string
  description = "Hetzner location to use for provisioned resources"
  default     = "hel1"
}

variable "datacenter" {
  type    = string
  default = "hel1-dc2"
}

variable "ssh_keys" {
  type        = list(number)
  description = "ssh keys to provision for instance access"
}

variable "server_type" {
  type        = string
  description = "Hetzner cloud server type, supports x86 and ARM architectures"
  default     = "cx23"
}

variable "s3_buckets" {
  type = list(object({
    name                     = string
    owner_key_id             = optional(string)
    owner_secret_key         = optional(string)
    ro_key_id                = optional(string)
    ro_secret_key            = optional(string)
    rw_key_id                = optional(string)
    rw_secret_key            = optional(string)
    web_access_public_enable = optional(bool, false)
    web_access_domains       = optional(list(string))
  }))
  default = []

  validation {
    condition = alltrue([
      for s3_bucket in var.s3_buckets : ((s3_bucket.owner_key_id == null || length(s3_bucket.owner_key_id) == 24) && (s3_bucket.owner_secret_key == null || length(s3_bucket.owner_secret_key) == 64))
    ])
    error_message = "All key ids must be 24 characters long. The secret key must have 64 hexadecimal characters."
  }
}

variable "data_volume_size" {
  type        = number
  description = "data volume size in GB"
  default     = 64
}

variable "labels" {
  type        = map(any)
  description = "A list of labels to be attached to the server instance."
  default     = {}
}

variable "docker_users" {
  type = list(object({
    username : string,
    password : string
  }))
  default = []
}

variable "allow_public_access" {
  type    = bool
  default = false
}

variable "dns_zone" {
  type = string
}
