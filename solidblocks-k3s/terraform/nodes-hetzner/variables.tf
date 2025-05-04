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

variable "server_count" {
  type    = number
  default = 3
}

variable "agent_count" {
  type    = number
  default = 1
}

variable "ssh_key_id" {
  type = number
}

variable "network_id" {
  type = number
}

variable "network_zone" {
  type        = string
  description = "network zone, eu-central, us-east, us-west."
}

variable "nodes_cidr" {
  description = "CIDR of the loadbalancer subnet"
  type        = string
}