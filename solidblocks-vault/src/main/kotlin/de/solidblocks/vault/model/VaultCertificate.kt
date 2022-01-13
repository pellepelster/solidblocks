package de.solidblocks.vault.model

import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

val certificateFactory = CertificateFactory.getInstance("X.509")

data class VaultCertificate(val certificateRaw: String, val privateKeyRaw: String, val issuingCaRaw: String) {

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
