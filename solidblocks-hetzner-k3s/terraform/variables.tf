variable "location" {
  type    = string
  default = "nbg1"
}

variable "server_count" {
  type    = number
  default = 3
}

variable "name" {
  type = string
}

variable "environment" {
  type    = string
  default = "test"
}

# https://github.com/hetznercloud/hcloud-cloud-controller-manager/blob/main/docs/deploy_with_networks.md#considerations-on-the-ip-ranges

variable "network_cidr" {
  description = "CIDR of the private network."
  type        = string
  default     = "10.0.0.0/8"
}


variable "cluster_cidr_network_bits" {
  description = "Cluster network CIDR bits."
  type        = number
  default     = 16
}

variable "service_cidr_network_offset" {
  description = "Service CIDR."
  type        = number
  default     = 43
}

variable "service_cidr_network_bits" {
  description = "Service network CIDR bits."
  type        = number
  default     = 16
}

variable "cluster_cidr_network_offset" {
  description = "Cluster network offset."
  type        = number
  default     = 224
}


variable "private_subnet_cidr" {
  description = "CIDR of the private network."
  type        = string
  default     = "10.0.1.0/24"
}

variable "network_zone" {
  description = "Network zone, eu-central, us-east, us-west."
  type        = string
  default     = "eu-central"
}
