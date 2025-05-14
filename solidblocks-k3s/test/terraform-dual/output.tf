module "k3s_ansible_output_blue" {
  source      = "../../terraform/output-ansible"
  output_path = "${path.root}/output/${local.environment}/${local.name_blue}"

  environment = local.environment

  k3s_api_endpoint    = "${hetznerdns_record.k3s_api_blue.name}.${data.hetznerdns_zone.blcks_de.name}"
  k3s_api_endpoint_ip = module.network_loadbalancer_blue.k3s_api_loadbalancer_ipv4_address

  k3s_token = module.k3s_nodes_blue.ks_token
  name      = local.name_blue

  k3s_servers = module.k3s_nodes_blue.servers
  k3s_agents  = module.k3s_nodes_blue.agents

  cluster_cidr = local.cluster_cidr_blue
  network_cidr = local.network_cidr
  nodes_cidr   = local.nodes_cidr_blue
  service_cidr = local.service_cidr_blue

  ssh_config_file = module.ssh_config_blue.ssh_config_file
}

module "k3s_ansible_output_green" {
  source      = "../../terraform/output-ansible"
  output_path = "${path.root}/output/${local.environment}/${local.name_green}"

  environment         = local.environment
  k3s_api_endpoint    = "${hetznerdns_record.k3s_api_green.name}.${data.hetznerdns_zone.blcks_de.name}"
  k3s_api_endpoint_ip = module.network_loadbalancer_green.k3s_api_loadbalancer_ipv4_address

  k3s_token = module.k3s_nodes_green.ks_token
  name      = local.name_green

  k3s_servers = module.k3s_nodes_green.servers
  k3s_agents  = module.k3s_nodes_green.agents

  cluster_cidr = local.cluster_cidr_green
  network_cidr = local.network_cidr
  nodes_cidr   = local.nodes_cidr_green
  service_cidr = local.service_cidr_green

  ssh_config_file = module.ssh_config_green.ssh_config_file
}

module "ansible_hetzner_output_blue" {
  source      = "../../terraform/output-ansible-hetzner"
  output_path = "${path.root}/output/${local.environment}/${local.name_blue}"

  hcloud_token = var.hcloud_token
  network_id   = module.network.network_id
}

module "ansible_hetzner_output_green" {
  source      = "../../terraform/output-ansible-hetzner"
  output_path = "${path.root}/output/${local.environment}/${local.name_green}"

  hcloud_token = var.hcloud_token
  network_id   = module.network.network_id
}

module "ssh_config_blue" {
  source      = "../../terraform/output-ssh-config"
  output_path = "${path.root}/output/${local.environment}/${local.name_blue}"

  ssh_private_key_openssh = module.ssh_hetzner.ssh_private_key_openssh
  ssh_servers             = concat(module.k3s_nodes_blue.servers, module.k3s_nodes_blue.agents)
}

module "ssh_config_green" {
  source      = "../../terraform/output-ssh-config"
  output_path = "${path.root}/output/${local.environment}/${local.name_green}"

  ssh_private_key_openssh = module.ssh_hetzner.ssh_private_key_openssh
  ssh_servers             = concat(module.k3s_nodes_green.servers, module.k3s_nodes_green.agents)
}
