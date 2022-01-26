package de.solidblocks.ingress.agent

import de.solidblocks.base.resources.ServiceResource
import java.io.File

data class ReverseProxyConfiguration(
    val reference: ServiceResource,
    val upstream: String,
    val hostnames: List<String>,
    val clientCertificateFile: File,
    val clientCertificateKeyFile: File,
    val rootCAPemFile: File
)
