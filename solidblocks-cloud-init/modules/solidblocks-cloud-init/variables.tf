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