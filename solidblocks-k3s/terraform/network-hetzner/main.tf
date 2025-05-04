locals {
  labels = {
    "blcks.de/name" : "k3s",
    "blcks.de/environment" : var.environment,
  }
}

resource "hcloud_network" "network" {
  ip_range                 = var.network_cidr
  name                     = "${var.environment}-${var.name}"
  expose_routes_to_vswitch = true
  delete_protection        = true
  labels                   = merge(local.labels, var.labels)
}

resource "hcloud_network_subnet" "load_balancers" {
  network_id   = hcloud_network.network.id
  type         = "cloud"
  network_zone = var.network_zone
  ip_range     = var.load_balancers_subnet_cidr
}

resource "hcloud_load_balancer" "k3s_api" {
  load_balancer_type = "lb11"
  name               = "${var.environment}-${var.name}-k3s"
  location           = var.location
  delete_protection  = true
  labels             = merge(local.labels, var.labels)
}

resource "hcloud_load_balancer_network" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  network_id       = hcloud_network.network.id
  ip               = cidrhost(hcloud_network_subnet.load_balancers.ip_range, 1)
}

resource "hcloud_load_balancer_target" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  type             = "label_selector"
  label_selector   = "blcks.de/name=k3s,blcks.de/node=master"
  use_private_ip   = true
  depends_on       = [hcloud_load_balancer_network.k3s_api]
}

resource "hcloud_load_balancer_service" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  protocol         = "tcp"
  listen_port      = 6443
  destination_port = 6443
}

resource "hcloud_load_balancer_service" "k3s_ssh" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  protocol         = "tcp"
  listen_port      = 22
  destination_port = 22
}

resource "hcloud_load_balancer" "ingress_default" {
  load_balancer_type = "lb11"
  name               = "${var.environment}-${var.name}-ingress-default"
  location           = var.location
  delete_protection  = true
  labels             = merge(local.labels, var.labels)
}

resource "hcloud_load_balancer_network" "ingress_default" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  network_id       = hcloud_network.network.id
  ip               = cidrhost(hcloud_network_subnet.load_balancers.ip_range, 2)
}

resource "hcloud_load_balancer_target" "ingress_default" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  type             = "label_selector"
  label_selector   = "blcks.de/name=k3s,blcks.de/node=agent"
  use_private_ip   = true

  depends_on = [hcloud_load_balancer_network.ingress_default]
}

resource "hcloud_load_balancer_service" "ingress_default_http" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 8080
  proxyprotocol    = true
}

resource "hcloud_load_balancer_service" "ingress_default_https" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  protocol         = "tcp"
  listen_port      = 443
  destination_port = 8080
  proxyprotocol    = true
}

resource "hcloud_firewall" "k3s" {
  name   = "${var.environment}-k3s"
  labels = merge(local.labels, var.labels)

  apply_to {
    label_selector = "blcks.de/name=k3s"
  }

  rule {
    direction  = "in"
    protocol   = "icmp"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }

  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "22"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
}