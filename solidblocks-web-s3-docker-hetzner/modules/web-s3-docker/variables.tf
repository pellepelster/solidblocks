variable "name" {
  type        = string
  description = "Unique name for the server instance"
}

variable "dns_zone" {
  type        = string
  description = "DNS zone to use for hostname and DNs entries"
}

variable "location" {
  type        = string
  description = "Hetzner location to use for provisioned resources"
  default     = "nbg1"
}

variable "datacenter" {
  type    = string
  default = "nbg1-dc3"
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

  description = "S3 buckets to provision, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#s3-buckets for details"
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

variable "docker_rw_users" {
  type = list(object({
    username : string,
    password : optional(string)
  }))
  default     = []
  description = "Docker read-write users to provision. If no users is given a user will be auto.created, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#docker for details"
}

variable "docker_public_enable" {
  type        = bool
  default     = false
  description = "Enable public anonymous access to Docker registry, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#docker for details"
}

variable "docker_enable" {
  type        = bool
  description = "Enable Docker registry"
  default     = false
}

variable "docker_ro_users" {
  type = list(object({
    username : string,
    password : optional(string)
  }))
  default     = []
  description = "Docker read-write users to provision. If no users is given a user will be auto.created, see https://pellepelster.github.io/solidblocks/hetzner/web-s3-docker/#docker for details"
}

variable "ssh_host_key_ed25519" {
  type        = string
  description = "override generated ssh host ed25519 key, must be set alongside with 'ssh_host_cert_ed25519'"
  default     = ""
}

variable "ssh_host_cert_ed25519" {
  type        = string
  description = "override generated ssh host ed25519 certificate, must be set alongside with 'ssh_host_key_ed25519'"
  default     = ""
}

variable "ssh_host_key_rsa" {
  type        = string
  description = "override generated ssh host ed25519 key, must be set alongside with 'ssh_host_cert_rsa'"
  default     = ""
}

variable "ssh_host_cert_rsa" {
  type        = string
  description = "override generated ssh host ed25519 certificate, must be set alongside with 'ssh_host_key_rsa'"
  default     = ""
}

variable "ssh_host_key_ecdsa" {
  type        = string
  description = "override generated ssh host ed25519 key, must be set alongside with 'ssh_host_cert_ecdsa'"
  default     = ""
}

variable "ssh_host_cert_ecdsa" {
  type        = string
  description = "override generated ssh host ed25519 certificate, must be set alongside with 'ssh_host_key_ecdsa'"
  default     = ""
}

variable "disable_volume_delete_protection" {
  type    = bool
  default = false
}
