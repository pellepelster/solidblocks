variable "name" {
  type        = string
  description = "Name for the K3S cluster associated resources"
}

variable "environment" {
  type        = string
  description = "Environment/stage for the resources (prod, dev, staging)"
}

variable "location" {
  type        = string
  description = "Hetzner location for created resources (nbg1, fsn1, ...)"
  default     = "nbg1"
}

variable "labels" {
  type        = map(string)
  description = "additional labels for all created resources"
  default     = {}
}

variable "server_count" {
  type        = number
  description = "Number of K3S servers to create"
  default     = 3
}

variable "server_type" {
  type        = string
  description = "Hetzner server instance type for K3S servers"
  default     = "ccx13"
}

variable "agent_count" {
  type        = number
  description = "Number of K3S agents to create"
  default     = 1
}

variable "agent_type" {
  type        = string
  description = "Hetzner server instance type for K3S agents"
  default     = "ccx23"
}

variable "ssh_key_id" {
  type        = number
  description = "Hetzner id of the SSH key for the created servers"
}

variable "network_id" {
  type        = number
  description = "Hetzner network id for the created servers"
}

variable "network_zone" {
  type        = string
  description = "Hetzner network zone for network resources (eu-central, us-east, us-west)"
  default     = "eu-central"
}

variable "nodes_cidr" {
  description = "CIDR for the server subnet"
  type        = string
  default     = "10.0.1.0/24"
}
