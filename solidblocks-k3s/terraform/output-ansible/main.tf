resource "local_file" "inventory" {
  filename = "${var.output_path}/ansible/blcks_k3s_inventory.yml"
  content = templatefile("${path.module}/templates/ansible_inventory.template", {
    k3s_servers     = var.k3s_servers
    k3s_agents      = var.k3s_agents
    ssh_config_file = var.ssh_config_file
  })
}

resource "local_file" "variables" {
  filename = "${var.output_path}/ansible/blcks_k3s_variables.yml"
  content = templatefile("${path.module}/templates/ansible_variables.template", {
    k3s_name : var.name
    k3s_environment : var.environment
    k3s_token : var.k3s_token
    k3s_api_endpoint : var.k3s_api_endpoint
    k3s_api_endpoint_ip : var.k3s_api_endpoint_ip

    cluster_cidr : var.cluster_cidr
    service_cidr : var.service_cidr
    network_cidr : var.network_cidr
    nodes_cidr : var.nodes_cidr
  })
}
