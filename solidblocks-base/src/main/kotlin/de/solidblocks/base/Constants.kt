package de.solidblocks.base

class Constants {
    class ConfigKeys {
        companion object {
            const val GITHUB_TOKEN_RO_KEY = "github_token_ro"
            const val GITHUB_USERNAME_KEY = "github_username"

            const val HETZNER_CLOUD_API_TOKEN_RO_KEY = "hetzner_cloud_api_key_ro"
            const val HETZNER_CLOUD_API_TOKEN_RW_KEY = "hetzner_cloud_api_key_rw"

            const val HETZNER_DNS_API_TOKEN_RW_KEY = "hetzner_dns_api_key_rw"

            const val CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
            const val CONSUL_SECRET_KEY = "consul_secret"

            const val SSH_PUBLIC_KEY = "ssh_public_key"
            const val SSH_PRIVATE_KEY = "ssh_private_key"

            const val SSH_IDENTITY_PUBLIC_KEY = "ssh_identity_public_key"
            const val SSH_IDENTITY_PRIVATE_KEY = "ssh_identity_private_key"
        }
    }
}