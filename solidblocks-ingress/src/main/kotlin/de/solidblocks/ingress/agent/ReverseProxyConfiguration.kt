package de.solidblocks.ingress.agent

import de.solidblocks.base.reference.ServiceReference
import java.io.File

data class ReverseProxyConfiguration(
        val reference: ServiceReference,
        val upstream: String,
        val hostnames: List<String>,
        val clientCertificateFile: File,
        val clientCertificateKeyFile: File,
        val rootCAPemFile: File
)
