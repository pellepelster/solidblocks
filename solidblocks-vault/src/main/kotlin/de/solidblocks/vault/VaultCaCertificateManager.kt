package de.solidblocks.vault

import de.solidblocks.vault.model.VaultCaCertificate
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.URI
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class VaultCaCertificateManager(
    private val address: String,
    token: String,
    val pkiMount: String,
    private val minCertificateLifetime: Duration = Duration.days(2),
    private val checkInterval: Duration = Duration.minutes(10),
    private val callback: ((VaultCaCertificate) -> Unit)? = null,
) {

    private val logger = KotlinLogging.logger {}

    private val vaultTemplate: VaultTemplate

    var caCertificate: VaultCaCertificate? = null

    init {
        logger.info { "initializing vault manager for address '$address'" }
        vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(address)), TokenAuthentication(token))
        startWorker()
    }

    private fun startWorker() {
        logger.info { "starting ca certificate manager worker thread" }
        thread(start = true) {
            while (true) {
                if (caCertificate != null) {

                    val remainingCertificateLifetime = caCertificate!!.remainingCaCertificateLifetime
                    if (remainingCertificateLifetime > minCertificateLifetime) {
                        logger.info { "certificate still has ${remainingCertificateLifetime.inWholeHours} hours left" }
                    } else {
                        logger.info { "certificate has less than ${minCertificateLifetime.inWholeHours} hours left, fetching new certificate" }
                        updateCaCertificate()
                    }
                }

                if (caCertificate == null) {
                    updateCaCertificate()
                    Thread.sleep(Duration.seconds(10).inWholeMilliseconds)
                } else {
                    Thread.sleep(checkInterval.inWholeMilliseconds)
                }
            }
        }
    }

    private fun updateCaCertificate() {
        try {
            val path = "$pkiMount/cert/ca"

            logger.info { "reading ca certificate from '$path'" }
            val response = vaultTemplate.read(path)

            if (response?.data == null) {
                return
            }

            val certificateRaw = response.data!!["certificate"].toString()

            caCertificate = VaultCaCertificate(certificateRaw)
            callback?.invoke(caCertificate!!)
        } catch (e: Exception) {
            logger.error(e) { "failed to read ca certificate" }
        }
    }

    public fun waitForCaCertificate(): VaultCaCertificate {
        while (caCertificate == null) {
            logger.info { "waiting for ca certificate" }
            Thread.sleep(Duration.seconds(5).inWholeMilliseconds)
        }

        return caCertificate!!
    }
}
