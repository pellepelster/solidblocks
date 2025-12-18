variable "solidblocks_base_url" {
  type        = string
  default     = null
  description = "Override base url for bootstrapping Solidblocks cloud-init for integration testing purposes"
}

variable "storage" {
  description = "List of `linux_device`s to mount into `mount_path`s, see [documentation](https://pellepelster.github.io/solidblocks/cloud-init/functions/storage/#storage_mount)"
  type = list(object({
    linux_device = string
    mount_path   = string
  }))
  default = [
  ]
}

variable "acme_ssl" {
  description = "Configure ACME certificate retrieval from LetsEncrypt, see [documentation](https://pellepelster.github.io/solidblocks/cloud-init/functions/lego/#lego_setup_dns)"
  type = object({
    path         = string
    email        = string
    domains      = list(string)
    acme_server  = string
    dns_provider = string
    variables    = map(string)
  })
  default = null
}
