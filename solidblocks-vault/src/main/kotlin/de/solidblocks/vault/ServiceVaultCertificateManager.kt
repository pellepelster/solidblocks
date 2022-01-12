package de.solidblocks.vault

import de.solidblocks.base.ServiceReference
import de.solidblocks.vault.VaultConstants.domain
import de.solidblocks.vault.VaultConstants.pkiMountName
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ServiceVaultCertificateManager constructor(
    address: String,
    token: String,
    reference: ServiceReference,
    rootDomain: String,
    isDevelopment: Boolean = false,
    minCertificateLifetime: Duration = Duration.days(2),
    checkInterval: Duration = Duration.minutes(10)
) : BaseVaultCertificateManager(
    address,
    token,
    pkiMountName(reference),
    domain(reference, rootDomain),
    isDevelopment,
    minCertificateLifetime,
    checkInterval
)
