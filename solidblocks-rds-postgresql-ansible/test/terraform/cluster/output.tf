module "ansible_output" {
  source      = "../../../terraform/output-ansible"
  output_path = "${path.root}/../output/${var.environment}/cluster"

  environment     = var.environment
  ssh_config_file = module.ssh_config.ssh_config_file
  name            = var.name

  databases = {
    "database2" = {
      servers = [
        {
          name              = hcloud_server.database2_blue.name,
          ipv4_address      = hcloud_server.database2_blue.ipv4_address,
          data_linux_device = data.hcloud_volume.database2_blue_data.linux_device
        },
        {
          name              = hcloud_server.database2_green.name,
          ipv4_address      = hcloud_server.database2_green.ipv4_address,
          data_linux_device = data.hcloud_volume.database2_green_data.linux_device
        }
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
  output_path = "${path.root}/../output/${var.environment}/cluster"

  ssh_private_key_openssh = module.ssh_hetzner.ssh_private_key_openssh
  ssh_servers = [hcloud_server.database2_green, hcloud_server.database2_blue]
}

output "database2_blue_name" {
  value = hcloud_server.database2_blue.name
}

output "database1_blue_ip" {
  value = hcloud_server.database2_blue.ipv4_address
}

output "database2_green_name" {
  value = hcloud_server.database2_green.name
}

output "database2_green_ip" {
  value = hcloud_server.database2_green.ipv4_address
}
