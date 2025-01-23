resource "local_file" "ssh_client_identity" {
  filename        = "${path.module}/output/${var.environment}_ssh_client_identity"
  content         = tls_private_key.ssh_client_identity.private_key_openssh
  file_permission = "0600"
}

resource "local_file" "ssh_config" {
  filename = "${path.module}/output/${var.environment}_ssh_config"
  content = templatefile("${path.module}/templates/ssh_config.template", {
    servers = hcloud_server.server[*]
    agents  = hcloud_server.agent[*]
    client_identity_file = abspath("${path.module}/${local_file.ssh_client_identity.filename}")
  }
  )
}

resource "local_file" "inventory" {
  filename = "${path.module}/output/${var.environment}_ansible_inventory.yml"
  content = templatefile("${path.module}/templates/inventory.template", {
    servers = hcloud_server.server[*]
    agents  = hcloud_server.agent[*]
    ssh_config_file = abspath("${path.module}/${local_file.ssh_config.filename}")
  })
}

resource "local_file" "variables" {
  filename = "${path.module}/output/${var.environment}_ansible_variables.yml"
  content = templatefile("${path.module}/templates/variables.template", {
    k3s_name : var.name
    k3s_environment : var.environment
    k3s_token : random_string.k3s_token.id

    k3s_api_endpoint : hcloud_load_balancer.k3s_api.ipv4
    cluster_cidr_network : cidrsubnet(var.network_cidr, var.cluster_cidr_network_bits - 8, var.cluster_cidr_network_offset)
    service_cidr_network : cidrsubnet(var.network_cidr, var.service_cidr_network_bits - 8, var.service_cidr_network_offset)
    private_cidr_network : hcloud_network_subnet.subnet.ip_range
    network_id : hcloud_network.network.id
  })
}

data "hetznerdns_zone" "solidblocks" {
  name = "solidblocks.de"
}

resource "random_string" "k3s_token" {
  length  = 48
  upper   = false
  special = false
}
