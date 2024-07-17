variable "solidblocks_base_url" {
  type    = string
  default = null
}

variable "storage" {
  type = list(object({
    linux_device = string
    mount_path   = string
  }))
  default = [
  ]
  description = "list of linux devices to mount into mount paths"
}

variable "acme_ssl" {
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
