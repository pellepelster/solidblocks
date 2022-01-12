package de.solidblocks.vault

import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

val certificateFactory = CertificateFactory.getInstance("X.509")

data class Certificate(val certificateRaw: String, val privateKeyRaw: String, val issuingCaRaw: String) {

    val public: X509Certificate
        get() {
            return certificateFactory.generateCertificate(certificateRaw.byteInputStream()) as X509Certificate
        }

    @OptIn(ExperimentalTime::class)
    val remainingCertificateLifetime: Duration
        get() {
            val currentEpocSeconds = public.notAfter.toInstant().toEpochMilli()
            return (currentEpocSeconds - Instant.now().toEpochMilli()).toDuration(DurationUnit.MILLISECONDS)
        }
}

@OptIn(ExperimentalTime::class)
abstract class BaseVaultCertificateManager(
    private val address: String,
    token: String,
    private val pkiMount: String,
    private val commonName: String,
    private val isDevelopment: Boolean = false,
    private val minCertificateLifetime: Duration = Duration.days(2),
    private val checkInterval: Duration = Duration.minutes(10)
) {

    private val logger = KotlinLogging.logger {}

    private val vaultTemplate: VaultTemplate

    var certificate: Certificate? = null

    init {
        logger.info { "initializing vault manager for address '$address'" }
        vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(address)), TokenAuthentication(token))

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

                Thread.sleep(checkInterval.inWholeMilliseconds)
            }
        }
    }

    fun altNames() = if (isDevelopment) {
        listOf("localhost").joinToString(",")
    } else {
        emptyList<String>().joinToString(",")
    }

    fun ipAddresses() =
        NetworkInterface.getNetworkInterfaces().asSequence().flatMap { it.inetAddresses.asSequence() }.filterIsInstance(
            Inet4Address::class.java
        ).map { it.hostAddress }

    fun issueCertificate(): Certificate? {
        try {
            val path = "$pkiMount/issue/$pkiMount"

            logger.info { "issuing certificate for '$commonName at '$path'" }
            val response = vaultTemplate.write(
                path,
                mapOf(
                    "common_name" to commonName,
                    "alt_names" to altNames(),
                    "ip_sans" to ipAddresses()

                )
            )

            if (response?.data == null) {
                return null
            }

            val certificate = response.data!!["certificate"].toString()
            val privateKey = response.data!!["private_key"].toString()
            val issuingCa = response.data!!["issuing_ca"].toString()
            val serialNumber = response.data!!["serial_number"].toString()

            val result = Certificate(certificate, privateKey, issuingCa)
            logger.info { "issued certificate '${result.public.serialNumber}' valid until ${result.public.notAfter}" }
            return result
        } catch (e: Exception) {
            logger.error(e) { "failed to issue certificate for service '$commonName'" }
        }

        return null
    }

    fun seal(): Boolean {
        logger.info { "sealing vault at address '$address'" }
        vaultTemplate.opsForSys().seal()
        return true
    }
}
