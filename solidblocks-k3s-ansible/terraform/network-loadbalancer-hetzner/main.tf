locals {
  labels = {
    "blcks.de/name" : var.name,
    "blcks.de/environment" : var.environment,
  }
}

locals {
  base_name = "${var.environment}-${var.name}"
}

resource "hcloud_load_balancer" "k3s_api" {
  load_balancer_type = "lb11"
  name               = "${local.base_name}-k3s-api"
  location           = var.location
  delete_protection  = true
  labels             = merge(local.labels, var.labels)
}

resource "hcloud_load_balancer_network" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  network_id       = var.network_id
  ip               = cidrhost(var.load_balancers_subnet_cidr, var.load_balancers_subnet_offset + 1)
}

resource "hcloud_load_balancer_target" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  type             = "label_selector"
  label_selector   = "blcks.de/name=${var.name},blcks.de/environment=${var.environment},blcks.de/k3s-node-type=server"
  use_private_ip   = true
  depends_on       = [hcloud_load_balancer_network.k3s_api]
}

resource "hcloud_load_balancer_service" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  protocol         = "tcp"
  listen_port      = 6443
  destination_port = 6443
}

resource "hcloud_load_balancer" "k3s_ingress_default" {
  load_balancer_type = "lb11"
  name               = "${local.base_name}-k3s-ingress-default"
  location           = var.location
  delete_protection  = true
  labels             = merge(local.labels, var.labels)
}

resource "hcloud_load_balancer_network" "ingress_default" {
  load_balancer_id = hcloud_load_balancer.k3s_ingress_default.id
  network_id       = var.network_id
  ip               = cidrhost(var.load_balancers_subnet_cidr, var.load_balancers_subnet_offset + 2)
}

resource "hcloud_load_balancer_target" "ingress_default" {
  load_balancer_id = hcloud_load_balancer.k3s_ingress_default.id
  type             = "label_selector"
  label_selector   = "blcks.de/name=${var.name},blcks.de/environment=${var.environment},blcks.de/k3s-node-type=agent"
  use_private_ip   = true
  depends_on       = [hcloud_load_balancer_network.ingress_default]
}

resource "hcloud_load_balancer_service" "ingress_default_http" {
  load_balancer_id = hcloud_load_balancer.k3s_ingress_default.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 8080
  proxyprotocol    = true
}

resource "hcloud_load_balancer_service" "ingress_default_https" {
  load_balancer_id = hcloud_load_balancer.k3s_ingress_default.id
  protocol         = "tcp"
  listen_port      = 443
  destination_port = 8080
  proxyprotocol    = true
}
