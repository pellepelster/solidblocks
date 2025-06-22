variable "name" {
  type        = string
  description = "resources base name"
}

variable "environment" {
  type        = string
  description = "environment/stage for the resources (prod, dev, staging)"
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
