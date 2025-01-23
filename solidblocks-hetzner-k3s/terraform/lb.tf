resource "hcloud_load_balancer" "ingress_default" {
  load_balancer_type = "lb11"
  name               = "${var.environment}-${var.name}-ingress-default"
  location           = var.location
  labels             = local.default_labels
}

resource "hcloud_load_balancer_network" "ingress_default" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  network_id       = hcloud_network.network.id
  ip = cidrhost(var.k3s_nodes_subnet_cidr, 10)
}

resource "hcloud_load_balancer_target" "ingress_default" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  type             = "label_selector"
  label_selector   = "${local.namespace}/component=server,${local.namespace}/part-of=k3s"
  use_private_ip   = true
}

resource "hcloud_load_balancer_service" "https_default" {
  load_balancer_id = hcloud_load_balancer.ingress_default.id
  protocol         = "https"
  listen_port      = 443
  destination_port = 8080

  health_check {
    interval = 10
    port     = 8080
    protocol = "tcp"
    timeout  = 5
  }

  http {
    certificates = [hcloud_managed_certificate.test_solidblocks_de.id]
  }
}

resource "hcloud_load_balancer" "k3s_api" {
  load_balancer_type = "lb11"
  name               = "${var.environment}-${var.name}-k3s"
  location           = var.location
  labels             = local.default_labels
}

resource "hcloud_load_balancer_network" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  network_id       = hcloud_network.network.id
  ip = cidrhost(var.k3s_nodes_subnet_cidr, 20)
}

resource "hcloud_load_balancer_target" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  type             = "label_selector"
  label_selector   = "${local.namespace}/component=server,${local.namespace}/part-of=k3s"
  use_private_ip   = true
}

resource "hcloud_load_balancer_service" "k3s_api" {
  load_balancer_id = hcloud_load_balancer.k3s_api.id
  protocol         = "tcp"
  listen_port      = 6443
  destination_port = 6443
}
