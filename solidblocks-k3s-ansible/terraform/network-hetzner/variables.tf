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

variable "network_cidr" {
  type        = string
  description = "CIDR for the private network"
  default     = "10.0.0.0/8"
}

variable "network_zone" {
  type        = string
  description = "Hetzner network zone for network resources (eu-central, us-east, us-west)"
  default     = "eu-central"
}

variable "load_balancers_subnet_cidr" {
  description = "CIDR for the loadbalancer subnet"
  type        = string
  default     = "10.0.2.0/24"
}