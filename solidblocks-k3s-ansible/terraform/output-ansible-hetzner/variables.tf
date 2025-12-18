variable "network_id" {
  type        = string
  description = "Hetzner resource id of the network"
}

variable "hcloud_token" {
  type        = string
  description = "Hetzner API token with R/W access"
}

variable "output_path" {
  type        = string
  description = "path for the generated files"
}