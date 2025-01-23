resource "hcloud_network" "network" {
  ip_range = var.network_cidr
  name     = "${var.environment}-${var.name}"
  labels   = local.default_labels
}

resource "hcloud_network_subnet" "subnet" {
  network_id   = hcloud_network.network.id
  type         = "cloud"
  network_zone = var.network_zone
  ip_range     = var.k3s_nodes_subnet_cidr
}

resource "hcloud_firewall" "k3s_nodes" {
  name   = "${var.environment}-${var.name}-nodes"
  labels = local.default_labels

  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "22"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }

  rule {
    direction  = "in"
    protocol   = "tcp"
    port       = "any"
    source_ips = [
      var.k3s_nodes_subnet_cidr,
    ]
  }

  rule {
    direction       = "out"
    protocol        = "tcp"
    port            = "any"
    destination_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
}

resource "hcloud_firewall_attachment" "k3s_nodes" {
  firewall_id     = hcloud_firewall.k3s_nodes.id
  label_selectors = ["${local.namespace}/part-of=k3s"]
}