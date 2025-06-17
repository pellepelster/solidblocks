variable "ssh_private_key_openssh" {
  type        = string
  description = "public key for server access in OpenSSH format"
}

variable "ssh_servers" {
  type = list(object({
    name         = string,
    user         = optional(string, "root"),
    ipv4_address = string
  }))
  description = "list of servers that can be accessed via the supplied `ssh_private_key_openssh`"
}

variable "output_path" {
  type        = string
  description = "path for the generated files"
}
