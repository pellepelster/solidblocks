data "template_file" "user_data" {

  template = file("../../../solidblocks-cloud-init/lib-cloud-init-generated/backup-cloud-init.sh")

  vars = {
    root_domain = var.root_domain
    environment = var.environment

    hostname    = "backup"
    public_ip   = hcloud_floating_ip.backup.ip_address

    solidblocks_version = var.solidblocks_version

    vault_token = vault_token.cloud_init.client_token

  }
}

data "hcloud_volume" "backup_storage" {
  name = "backup_storage_fsn1"
}

data "hetznerdns_zone" "dns_zone" {
  name = "blcks.de"
}


data "hcloud_ssh_key" "pelle" {
  name = "pelle"
}
