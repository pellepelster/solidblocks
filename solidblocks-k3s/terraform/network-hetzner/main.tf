locals {
  labels = {
    "blcks.de/name" : var.name,
    "blcks.de/environment" : var.environment,
  }
}

locals {
  base_name = "${var.environment}-${var.name}"
}

resource "hcloud_network" "network" {
  ip_range                 = var.network_cidr
  name                     = local.base_name
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

resource "hcloud_firewall" "k3s" {
  name   = local.base_name
  labels = merge(local.labels, var.labels)

  apply_to {
    label_selector = "blcks.de/name=${var.name},blcks.de/environment=${var.environment}"
  }

  rule {
    direction = "in"
    protocol  = "icmp"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "22"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
}