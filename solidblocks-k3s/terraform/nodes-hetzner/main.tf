locals {
  labels = {
    "blcks.de/name" : "k3s",
    "blcks.de/environment" : var.environment,
  }
}

resource "hcloud_volume" "k3s_server_data" {
  location          = var.location
  count             = var.server_count
  name              = "${var.environment}-${var.name}-k3s-server-data-${count.index}"
  size              = 32
  format            = "ext4"
  delete_protection = true
  labels            = merge(local.labels, var.labels)
}

resource "hcloud_network_subnet" "k3s_nodes" {
  network_id   = var.network_id
  type         = "cloud"
  network_zone = var.network_zone
  ip_range     = var.nodes_cidr
}


resource "hcloud_server" "k3s_server" {
  count    = var.server_count
  name     = "${var.environment}-${var.name}-k3s-server-${count.index}"
  location = var.location

  ssh_keys           = [var.ssh_key_id]
  delete_protection  = true
  rebuild_protection = true

  user_data = templatefile("${path.module}/user_data.sh.template", {
    user_data_lib       = file("${path.module}/user_data_lib.sh")
    storage_data_device = hcloud_volume.k3s_server_data[count.index].linux_device
  }
  )

  network {
    network_id = var.network_id
    ip         = cidrhost(var.nodes_cidr, count.index + 1)
  }

  image       = "debian-12"
  server_type = "ccx13"

  public_net {
    ipv4_enabled = true
    ipv6_enabled = false
  }

  labels = merge(local.labels, var.labels, {
    "blcks.de/name" : "k3s",
    "blcks.de/node" : "master",
  })
}

resource "hcloud_volume_attachment" "k3s_server_data" {
  count     = var.server_count
  server_id = hcloud_server.k3s_server[count.index].id
  volume_id = hcloud_volume.k3s_server_data[count.index].id
}

resource "random_string" "k3s_token" {
  length  = 48
  upper   = false
  special = false
}

resource "hcloud_server" "k3s_agent" {
  count    = var.agent_count
  name     = "${var.environment}-${var.name}-k3s-agent-${count.index}"
  location = var.location

  ssh_keys = [var.ssh_key_id]

  delete_protection  = true
  rebuild_protection = true

  network {
    network_id = var.network_id
    ip         = cidrhost(var.nodes_cidr, count.index + 10)
  }

  image       = "debian-12"
  server_type = "ccx23"

  public_net {
    ipv4_enabled = true
    ipv6_enabled = false
  }

  labels = merge(local.labels, var.labels, {
    "blcks.de/name" : "k3s",
    "blcks.de/node" : "agent",
  })
}
