package de.solidblocks.ingress.agent

import de.soldiblocks.ingress.api.ServiceIngressRequest
import de.solidblocks.base.BaseConstants.tenantId
import de.solidblocks.base.reference.EnvironmentServiceReference
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants.clientFQDN
import de.solidblocks.vault.VaultConstants.environmentClientPkiMountName
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName
import mu.KotlinLogging
import java.io.File
import java.net.InetAddress

class IngressManager(
    val vaultAddress: String,
    val vaultToken: String,
    val reference: EnvironmentServiceReference,
    val certificatesDir: File,
    val network: String? = null
) {

    private val caCertificateManagers = mutableMapOf<String, VaultCaCertificateManager>()

    private val serverCaFiles = mutableMapOf<String, File>()

    val clientPrivateKeyFile = File(certificatesDir, "client.key")

    val clientCertificateFile = File(certificatesDir, "client.cert")

    private val clientCertificateManager: VaultCertificateManager

    private val logger = KotlinLogging.logger {}

    val caddyManager: CaddyManager

    var services: List<ServiceIngressRequest> = emptyList()

    init {
        clientCertificateManager = VaultCertificateManager(
            address = vaultAddress,
            token = vaultToken,
            pkiMount = environmentClientPkiMountName(reference),
            commonName = clientFQDN(InetAddress.getLocalHost().hostName)
        ) {
            logger.info { "writing private key to '$clientPrivateKeyFile'" }
            clientPrivateKeyFile.writeText(it.privateKeyRaw)

            logger.info { "writing certificate to '$clientCertificateFile'" }
            clientCertificateFile.writeText(it.certificateRaw)
            updateCaddy()
        }

        caddyManager = CaddyManager(
            reference,
            certificatesDir,
            network
        )
    }

    fun updateCaddy() {
        logger.info { "updating caddy configuration" }

        val reverseProxyConfigurations = services.map {

            if (clientCertificateManager.certificate == null) {
                return@map null
            }

            if (!serverCaFiles.containsKey(tenantId(it.reference))) {
                return@map null
            }

            return@map ReverseProxyConfiguration(
                reference = it.reference,
                upstream = it.upstream,
                hostnames = it.hostnames,
                clientCertificateFile = clientCertificateFile,
                clientCertificateKeyFile = clientPrivateKeyFile,
                rootCAPemFile = serverCaFiles.get(tenantId(it.reference))!!
            )
        }.filterNotNull()

        caddyManager.updateServices(reverseProxyConfigurations)
    }

    fun updateServices(services: List<ServiceIngressRequest>) {
        this.services = services

        services.forEach { tenant ->
            caCertificateManagers.getOrPut(tenantId(tenant.reference)) {
                VaultCaCertificateManager(
                    address = vaultAddress,
                    token = vaultToken,
                    pkiMount = tenantServerPkiMountName(tenant.reference),
                ) {
                    val caCertFile = serverCaFiles.getOrPut(tenantId(tenant.reference)) { File(certificatesDir, "${tenantId(tenant.reference)}.cert") }

                    logger.info { "writing ca file to '$caCertFile'" }
                    caCertFile.writeText(it.caCertificateRaw)
                    updateCaddy()
                }
            }
        }
    }

    fun start() = caddyManager.start()
    fun httpPort() = caddyManager.httpPort()
}
