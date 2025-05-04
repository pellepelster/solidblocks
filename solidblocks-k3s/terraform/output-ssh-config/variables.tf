variable "ssh_private_key_openssh" {
  type = string
}

variable "ssh_servers" {
  type = list(object({
    name         = string,
    user         = optional(string, "root"),
    ipv4_address = string
  }))
}

variable "output_path" {
  type = string
}
