package de.solidblocks.ingress.agent.config

import com.fasterxml.jackson.annotation.JsonProperty

data class Tls(@JsonProperty("root_ca_pem_files") val rootCAPemFiles: List<String>)
