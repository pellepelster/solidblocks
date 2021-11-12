package de.solidblocks.provisioner.hetzner.cloud

import com.fasterxml.jackson.annotation.JsonProperty

data class HetznerProviderConfig(@JsonProperty("hetzner_cloud_api_token") val hetznerCloudApiToken: String)
