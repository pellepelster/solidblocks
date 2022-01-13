package de.solidblocks.vault

import de.solidblocks.vault.model.VaultCertificate
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class VaultCertificateManager(
    private val address: String,
    token: String,
    val pkiMount: String,
    val commonName: String,
    private val altNames: List<String> = emptyList(),
    private val minCertificateLifetime: Duration = Duration.days(2),
    private val checkInterval: Duration = Duration.minutes(10)
) {

    private val logger = KotlinLogging.logger {}

    private val vaultTemplate: VaultTemplate

    var certificate: VaultCertificate? = null

    init {
        logger.info { "initializing vault manager for address '$address'" }
        vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(address)), TokenAuthentication(token))
        startCertificateWorker()
    }

    private fun ipAddresses() =
        NetworkInterface.getNetworkInterfaces().asSequence().flatMap { it.inetAddresses.asSequence() }.filterIsInstance(
            Inet4Address::class.java
        ).map { it.hostAddress }

    private fun startCertificateWorker() {
        logger.info { "starting certificate manager worker thread" }
        thread(start = true) {
            while (true) {
                if (certificate != null) {

                    val remainingCertificateLifetime = certificate!!.remainingCertificateLifetime
                    if (remainingCertificateLifetime > minCertificateLifetime) {
                        logger.info { "certificate still has ${remainingCertificateLifetime.inWholeHours} hours left" }
                    } else {
                        logger.info { "certificate has less than ${minCertificateLifetime.inWholeHours} hours left" }
                        certificate = issueCertificate()
                    }
                }

                if (certificate == null) {
                    logger.info { "no active certificate found" }
                    certificate = issueCertificate()
                }

                if (certificate == null) {
                    Thread.sleep(Duration.seconds(10).inWholeMilliseconds)
                } else {
                    Thread.sleep(checkInterval.inWholeMilliseconds)
                }
            }
        }
    }

    private fun issueCertificate(): VaultCertificate? {
        try {
            val path = "$pkiMount/issue/$pkiMount"

            logger.info { "issuing certificate for '$commonName at '$path' with common name '$commonName' and alt names ${altNames.joinToString(", ") { "'$it'" }.ifEmpty { "<none>" }}" }
            val response = vaultTemplate.write(
                path,
                mapOf(
                    "common_name" to commonName,
                    "alt_names" to altNames.joinToString(","),
                    "ip_sans" to ipAddresses(),
                    "private_key_format" to "pkcs8"
                )
            )

            if (response?.data == null) {
                return null
            }

            val certificate = response.data!!["certificate"].toString()
            val privateKey = response.data!!["private_key"].toString()
            val issuingCa = response.data!!["issuing_ca"].toString()
            val serialNumber = response.data!!["serial_number"].toString()

            val result = VaultCertificate(certificate, privateKey, issuingCa)
            logger.info { "issued certificate '${result.public.serialNumber}' valid until ${result.public.notAfter}" }
            return result
        } catch (e: Exception) {
            logger.error(e) { "failed to issue certificate for service '$commonName'" }
        }

        return null
    }

    public fun waitForCertificate(): VaultCertificate {
        while (certificate == null) {
            logger.info { "waiting for first certificate" }
            Thread.sleep(Duration.seconds(5).inWholeMilliseconds)
        }

        return certificate!!
    }
}
