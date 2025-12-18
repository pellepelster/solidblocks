module "ansible_output" {
  source      = "../../../terraform/output-ansible"
  output_path = "${path.root}/../output/${var.environment}/standalone"

  environment     = var.environment
  ssh_config_file = module.ssh_config.ssh_config_file
  name            = var.name

  databases = {
    "database1" = {
      servers = [
        {
          name              = hcloud_server.database1_blue.name,
          ipv4_address      = hcloud_server.database1_blue.ipv4_address,
          data_linux_device = data.hcloud_volume.database1_blue_data.linux_device
        },
      ]
    }
  }
  backup_s3_bucket     = var.backup_s3_bucket
  backup_s3_endpoint   = var.backup_s3_endpoint
  backup_s3_key        = var.backup_s3_key
  backup_s3_key_secret = var.backup_s3_key_secret
  backup_s3_region     = var.backup_s3_region
}

module "ssh_config" {
  source      = "../../../terraform/output-ssh-config"
  output_path = "${path.root}/../output/${var.environment}/standalone"

  ssh_private_key_openssh = module.ssh_hetzner.ssh_private_key_openssh
  ssh_servers             = [hcloud_server.database1_blue]
}

output "database1_blue_name" {
  value = hcloud_server.database1_blue.name
}

output "database1_blue_ip" {
  value = hcloud_server.database1_blue.ipv4_address
}

