package de.solidblocks.ingress.agent.config

import com.fasterxml.jackson.annotation.JsonProperty

data class Tls(
    @JsonProperty("client_certificate_file") val clientCertificateFile: String,
    @JsonProperty("client_certificate_key_file") val clientCertificateKeyFile: String,
    @JsonProperty("root_ca_pem_files") val rootCAPemFiles: List<String>
)
