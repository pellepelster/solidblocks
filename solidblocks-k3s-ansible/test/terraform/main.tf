locals {
  location    = "nbg1"
  environment = "test"
  name        = "cluster1"

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

module "network" {
  source                     = "../../terraform/network-hetzner"
  environment                = local.environment
  location                   = local.location
  name                       = local.name
  labels                     = local.default_labels
  network_zone               = local.network_zone
  network_cidr               = local.network_cidr
  load_balancers_subnet_cidr = local.load_balancers_cidr
}

module "network_loadbalancer" {
  source                       = "../../terraform/network-loadbalancer-hetzner"
  environment                  = local.environment
  name                         = local.name
  network_id                   = module.network.network_id
  load_balancers_subnet_cidr   = local.load_balancers_cidr
  load_balancers_subnet_offset = 0
}

resource "hetznerdns_record" "k3s_api" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "k3s-api.${local.name}"
  value   = module.network_loadbalancer.k3s_api_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "k3s_ingress" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "k3s-ingress.${local.name}"
  value   = module.network_loadbalancer.ingress_default_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "hello_world" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "hello-world.${local.name}"
  value   = module.network_loadbalancer.ingress_default_loadbalancer_ipv4_address
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

module "k3s_nodes" {
  source       = "../../terraform/nodes-hetzner"
  environment  = local.environment
  location     = local.location
  name         = local.name
  labels       = local.default_labels
  network_id   = module.network.network_id
  ssh_key_id   = module.ssh_hetzner.root_ssh_key_id
  network_zone = local.network_zone
  nodes_cidr   = local.nodes_cidr
  agent_count  = 1
}
