data "template_file" "user_data" {

  template = file("user_data.sh")

  vars = {
    public_ip = hcloud_floating_ip.vault-1.ip_address
    hostname  = var.vault_1_hostname

    root_domain = var.root_domain
    environment = var.environment


    ssh_identity_ed25519_key = var.ssh_identity_ed25519_key
    ssh_identity_ed25519_pub = var.ssh_identity_ed25519_pub
  }
}

data "hcloud_volume" "vault-1_storage" {
  name = "vault-1_storage_fsn1"
}

data "hetznerdns_zone" "dns_zone" {
  name = "blcks.de"
}

data "hcloud_ssh_key" "pelle" {
  name = "pelle"
}
