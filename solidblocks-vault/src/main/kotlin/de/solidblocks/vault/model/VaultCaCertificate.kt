package de.solidblocks.vault.model

import java.security.cert.X509Certificate
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

data class VaultCaCertificate(val caCertificateRaw: String) {

    val caCertificate: X509Certificate
        get() {
            return certificateFactory.generateCertificate(caCertificateRaw.byteInputStream()) as X509Certificate
        }

    @OptIn(ExperimentalTime::class)
    val remainingCaCertificateLifetime: Duration
        get() {
            val currentEpocSeconds = caCertificate.notAfter.toInstant().toEpochMilli()
            return (currentEpocSeconds - Instant.now().toEpochMilli()).toDuration(DurationUnit.MILLISECONDS)
        }
}
