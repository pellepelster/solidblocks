locals {
  default_labels = {
    "blcks.de/environment" : var.environment
  }
}

module "ssh_hetzner" {
  source      = "../../../terraform/nodes-ssh-hetzner"
  environment = var.environment
  location    = var.location
  name        = var.name
  labels      = local.default_labels
}
