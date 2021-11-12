terraform {
  backend "local" {
    workspace_dir = "../../../terraform-state/vault-config/"
  }
  required_providers {
    vault = {
      source  = "hashicorp/vault"
      version = "2.24.1"
    }
  }
}

provider "vault" {
  address = "https://vault.${var.environment}.${var.root_domain}:8200"
  token   = var.vault_token
}

resource "vault_mount" "solidblocks" {
  path = "solidblocks"
  type = "kv-v2"
}

resource "vault_generic_secret" "cloud_init" {
  path = "${vault_mount.solidblocks.path}/nodes/cloud_init_config"

  data_json = <<EOT
{
  "github_token_ro": "${var.github_token_ro}"
}
EOT
}

resource "vault_policy" "cloud_init" {
  name = "cloud_init"

  policy = <<EOT
path "${vault_mount.solidblocks.path}/data/nodes/cloud_init_config" {
  capabilities = ["read"]
}
EOT
}
