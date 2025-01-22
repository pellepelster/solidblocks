resource "hcloud_volume" "server_data" {
  location = var.location
  count    = var.server_count
  name     = "${var.environment}-${var.name}-server-data-${count.index}"
  size     = 16
  format   = "ext4"
}

resource "hcloud_network" "network" {
  ip_range = var.network_cidr
  name     = "${var.environment}-${var.name}"
}

resource "hcloud_server" "server" {
  count    = var.server_count
  name     = "${var.environment}-${var.name}-server-${count.index}"
  location = var.location

  ssh_keys = [hcloud_ssh_key.root.id]

  user_data = templatefile("${path.module}/user_data.sh.template", {
    user_data_lib = file("${path.module}/user_data_lib.sh")
    storage_data_device = hcloud_volume.server_data[0].linux_device
  }
  )

  network {
    network_id = hcloud_network.network.id
    ip = cidrhost(var.private_subnet_cidr, count.index+1)
  }

  image       = "debian-12"
  server_type = "cx22"
}

resource "hcloud_volume_attachment" "server_data" {
  count     = var.server_count
  server_id = hcloud_server.server[count.index].id
  volume_id = hcloud_volume.server_data[count.index].id
}

resource "hcloud_floating_ip" "k3s_apiserver_endpoint" {
  type          = "ipv4"
  name          = "${var.environment}-${var.name}-server"
  home_location = var.location
}

resource "hcloud_floating_ip_assignment" "server_0" {
  floating_ip_id = hcloud_floating_ip.k3s_apiserver_endpoint.id
  server_id      = hcloud_server.server[0].id
}

resource "random_string" "k3s_token" {
  length  = 48
  upper   = false
  special = false
}

resource "hcloud_network_subnet" "subnet" {
  network_id   = hcloud_network.network.id
  type         = "cloud"
  network_zone = var.network_zone
  ip_range     = var.private_subnet_cidr
}

