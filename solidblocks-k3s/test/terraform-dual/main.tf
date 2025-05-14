locals {
  location     = "nbg1"
  environment  = "test"
  name         = "cluster1"
  name_blue    = "${local.name}-blue"
  name_green   = "${local.name}-green"
  network_zone = "eu-central"
  network_cidr = "10.0.0.0/8"

  load_balancers_cidr = "10.0.2.0/24"

  nodes_cidr_blue  = "10.0.1.0/24"
  nodes_cidr_green = "10.0.6.0/24"

  cluster_cidr_blue = "10.0.16.0/20"
  service_cidr_blue = "10.0.8.0/21"

  cluster_cidr_green = "10.0.32.0/20"
  service_cidr_green = "10.0.48.0/21"

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

module "network_loadbalancer_blue" {
  source                       = "../../terraform/network-loadbalancer-hetzner"
  environment                  = local.environment
  name                         = local.name_blue
  network_id                   = module.network.network_id
  load_balancers_subnet_cidr   = local.load_balancers_cidr
  load_balancers_subnet_offset = 0
}

module "network_loadbalancer_green" {
  source                       = "../../terraform/network-loadbalancer-hetzner"
  environment                  = local.environment
  name                         = local.name_green
  network_id                   = module.network.network_id
  load_balancers_subnet_cidr   = local.load_balancers_cidr
  load_balancers_subnet_offset = 10
}


resource "hetznerdns_record" "k3s_api_blue" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "k3s-api.${local.name_blue}"
  value   = module.network_loadbalancer_blue.k3s_api_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "k3s_ingress_blue" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "k3s-ingress.${local.name_blue}"
  value   = module.network_loadbalancer_blue.ingress_default_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "k3s_api_green" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "k3s-api.${local.name_green}"
  value   = module.network_loadbalancer_green.k3s_api_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "k3s_ingress_green" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "k3s-ingress.${local.name_green}"
  value   = module.network_loadbalancer_green.ingress_default_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "hello_world_blue" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "hello-world.${local.name_blue}"
  value   = module.network_loadbalancer_blue.ingress_default_loadbalancer_ipv4_address
  type    = "A"
  ttl     = 60
}

resource "hetznerdns_record" "hello_world_green" {
  zone_id = data.hetznerdns_zone.blcks_de.id
  name    = "hello-world.${local.name_green}"
  value   = module.network_loadbalancer_green.ingress_default_loadbalancer_ipv4_address
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

module "k3s_nodes_blue" {
  source       = "../../terraform/nodes-hetzner"
  environment  = local.environment
  location     = local.location
  name         = local.name_blue
  labels       = local.default_labels
  network_id   = module.network.network_id
  ssh_key_id   = module.ssh_hetzner.root_ssh_key_id
  network_zone = local.network_zone
  nodes_cidr   = local.nodes_cidr_blue
  agent_count  = 1
}

module "k3s_nodes_green" {
  source       = "../../terraform/nodes-hetzner"
  environment  = local.environment
  location     = local.location
  name         = local.name_green
  labels       = local.default_labels
  network_id   = module.network.network_id
  ssh_key_id   = module.ssh_hetzner.root_ssh_key_id
  network_zone = local.network_zone
  nodes_cidr   = local.nodes_cidr_green
  agent_count  = 1
}