locals {
  location     = "nbg1"
  environment  = "test"
  name         = "cluster1"
  network_zone = "eu-central"
  network_cidr = "10.0.0.0/8"

  nodes_cidr          = "10.0.1.0/24"
  load_balancers_cidr = "10.0.2.0/24"

  cluster_cidr = "10.0.16.0/20"
  service_cidr = "10.0.8.0/21"

  default_labels = {
    "blcks.de/environment" : local.environment
  }
}

module "network_hetzner" {
  source                     = "../../terraform/network-hetzner"
  environment                = local.environment
  location                   = local.location
  name                       = local.name
  labels                     = local.default_labels
  network_zone               = local.network_zone
  network_cidr               = local.network_cidr
  load_balancers_subnet_cidr = local.load_balancers_cidr
}

resource "hetznerdns_record" "k3s-api" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "test-k3s"
  value   = module.network_hetzner.k3s_api_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

module "ssh_hetzner" {
  source      = "../../terraform/nodes-ssh-hetzner"
  environment = local.environment
  location    = local.location
  name        = local.name
  labels      = local.default_labels
}

module "k3s_nodes_hetzner" {
  source       = "../../terraform/nodes-hetzner"
  environment  = local.environment
  location     = local.location
  name         = local.name
  labels       = local.default_labels
  network_id   = module.network_hetzner.network_id
  ssh_key_id   = module.ssh_hetzner.root_ssh_key_id
  network_zone = local.network_zone
  nodes_cidr   = local.nodes_cidr
  agent_count  = 1
}