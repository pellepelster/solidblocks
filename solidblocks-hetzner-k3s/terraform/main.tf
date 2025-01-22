locals {
  namespace = "blcks.de"
  default_labels = {
    "${local.namespace}/environment" : var.environment
    "${local.namespace}/name" : var.name
  }
}

resource "hcloud_volume" "server_data" {
  location = var.location
  count    = var.server_count
  name     = "${var.environment}-${var.name}-server-data-${count.index}"
  size     = 16
  format   = "ext4"
  labels   = local.default_labels
}

resource "hcloud_server" "server" {
  count    = var.server_count
  name     = "${var.environment}-${var.name}-server-${count.index}"
  location = var.location

  ssh_keys = [hcloud_ssh_key.root.id]

  user_data = templatefile("${path.module}/user_data.sh.template", {
    user_data_lib       = file("${path.module}/user_data_lib.sh")
    storage_data_device = hcloud_volume.server_data[0].linux_device
  }
  )

  network {
    network_id = hcloud_network.network.id
    ip         = cidrhost(var.private_subnet_cidr, count.index+1)
  }

  image       = "debian-12"
  server_type = "cx22"

  labels = merge(local.default_labels, {
    "${local.namespace}/part-of" : "k3s",
    "${local.namespace}/component" : "server",
  })
}

resource "hcloud_volume_attachment" "server_data" {
  count     = var.server_count
  server_id = hcloud_server.server[count.index].id
  volume_id = hcloud_volume.server_data[count.index].id
}

resource "hcloud_managed_certificate" "test_solidblocks_de" {
  domain_names = ["*.${var.name}.solidblocks.de", "${var.name}.solidblocks.de"]
  name         = "*.${var.name}.solidblocks.de"
  labels       = local.default_labels
}

resource "hetznerdns_record" "ingress_default" {
  zone_id = data.hetznerdns_zone.solidblocks.id
  name    = "*.${var.name}"
  value   = hcloud_load_balancer.ingress_default.ipv4
  type    = "A"
  ttl     = 60
}

resource "hcloud_server" "agent" {
  count    = var.server_count
  name     = "${var.environment}-${var.name}-agent-${count.index}"
  location = var.location

  ssh_keys = [hcloud_ssh_key.root.id]

  network {
    network_id = hcloud_network.network.id
    ip         = cidrhost(var.private_subnet_cidr, 20 + count.index+1)
  }

  image       = "debian-12"
  server_type = "cx22"

  labels = merge(local.default_labels, {
    "${local.namespace}/part-of" : "k3s",
    "${local.namespace}/component" : "agent",
  })
}
