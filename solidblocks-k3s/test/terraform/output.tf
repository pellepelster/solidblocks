module "k3s_ansible_output" {
  source      = "../../terraform/output-ansible"
  output_path = "${path.root}/output/${local.environment}/${local.name}"

  environment         = local.environment
  k3s_api_endpoint    = "${hetznerdns_record.k3s_api.name}.${data.hetznerdns_zone.blcks_de.name}"
  k3s_api_endpoint_ip = module.network_loadbalancer.k3s_api_loadbalancer_ipv4_address

  k3s_token = module.k3s_nodes.ks_token
  name      = local.name

  k3s_servers = module.k3s_nodes.servers
  k3s_agents  = module.k3s_nodes.agents

  cluster_cidr = local.cluster_cidr
  network_cidr = local.network_cidr
  nodes_cidr   = local.nodes_cidr
  service_cidr = local.service_cidr

  ssh_config_file = module.ssh_config.ssh_config_file
}

module "ansible_hetzner_output" {
  source      = "../../terraform/output-ansible-hetzner"
  output_path = "${path.root}/output/${local.environment}/${local.name}"

  hcloud_token = var.hcloud_token
  network_id   = module.network.network_id
}

module "ssh_config" {
  source      = "../../terraform/output-ssh-config"
  output_path = "${path.root}/output/${local.environment}/${local.name}"

  ssh_private_key_openssh = module.ssh_hetzner.ssh_private_key_openssh
  ssh_servers             = concat(module.k3s_nodes.servers, module.k3s_nodes.agents)
}
