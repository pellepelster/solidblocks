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
