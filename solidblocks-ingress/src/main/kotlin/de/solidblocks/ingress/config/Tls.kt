package de.solidblocks.ingress.config

import com.fasterxml.jackson.annotation.JsonProperty

data class Tls(@JsonProperty("root_ca_pem_files") val rootCAPemFiles: List<String>)
