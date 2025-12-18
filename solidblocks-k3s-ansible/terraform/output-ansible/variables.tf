variable "name" {
  type        = string
  description = "Name for the K3S cluster associated resources"
}

variable "environment" {
  type        = string
  description = "Environment/stage for the resources (prod, dev, staging)"
}

variable "k3s_token" {
  type        = string
  description = "K3S token, see https://docs.k3s.io/cli/token"
}

variable "k3s_api_endpoint" {
  type    = string
  default = "public address of the K3S api endpoint"
}

variable "k3s_api_endpoint_ip" {
  type    = string
  default = "ip address of the K3S api endpoint"
}

variable "cluster_cidr" {
  type        = string
  description = "CIDR for the K3S cluster, see https://github.com/hetznercloud/hcloud-cloud-controller-manager/blob/main/docs/deploy_with_networks.md#considerations-on-the-ip-ranges"
  default     = "10.0.16.0/20"
}

variable "service_cidr" {
  type        = string
  description = "CIDR for the K3S cluster services, see https://github.com/hetznercloud/hcloud-cloud-controller-manager/blob/main/docs/deploy_with_networks.md#considerations-on-the-ip-ranges"
  default     = "10.0.8.0/21"
}

variable "network_cidr" {
  type        = string
  description = "Hetzner id of the cluster network"
}

variable "nodes_cidr" {
  type        = string
  description = "CIDR for the K3S cluster nodes (servers and agents)"
}

variable "k3s_servers" {
  type        = list(object({ name = string, ipv4_address = string }))
  description = "K3S servers"
}

variable "k3s_agents" {
  type        = list(object({ name = string, ipv4_address = string }))
  description = "K3S agents"
}

variable "ssh_config_file" {
  type        = string
  description = "path to the ssh client config file for K3S node access"
}

variable "output_path" {
  type        = string
  description = "path for the generated files"
}