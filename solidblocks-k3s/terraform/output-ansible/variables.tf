variable "name" {
  type = string
}

variable "environment" {
  type = string
}

variable "k3s_token" {
  type = string
}

variable "k3s_api_endpoint" {
  type = string
}

variable "k3s_api_endpoint_ip" {
  type = string
}

variable "cluster_cidr" {
  type = string
}

variable "service_cidr" {
  type = string
}

variable "network_cidr" {
  type = string
}

variable "nodes_cidr" {
  type = string
}

variable "k3s_servers" {
  type = list(object({ name = string, ipv4_address = string }))
}

variable "k3s_agents" {
  type = list(object({ name = string, ipv4_address = string }))
}

variable "ssh_config_file" {
  type = string
}

variable "output_path" {
  type = string
}