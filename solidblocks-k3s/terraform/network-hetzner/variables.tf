variable "name" {
  type = string
}

variable "environment" {
  type = string
}

variable "location" {
  type = string
}

variable "labels" {
  type = map(string)
}

variable "network_cidr" {
  description = "CIDR of the private network"
  type        = string
}

variable "network_zone" {
  type        = string
  description = "network zone, eu-central, us-east, us-west."
}

variable "load_balancers_subnet_cidr" {
  description = "CIDR of the loadbalancer subnet"
  type        = string
}