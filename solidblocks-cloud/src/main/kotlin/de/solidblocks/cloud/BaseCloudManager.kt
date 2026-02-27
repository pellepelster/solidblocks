package de.solidblocks.cloud

import de.solidblocks.cloud.providers.hetzner.HetznerProviderRegistration
import de.solidblocks.cloud.providers.pass.PassProviderRegistration
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderRegistration
import de.solidblocks.cloud.services.s3.S3ServiceRegistration

abstract class BaseCloudManager {
    val serviceRegistrations = listOf(S3ServiceRegistration())

    val providerRegistrations =
        listOf(
            HetznerProviderRegistration(),
            PassProviderRegistration(),
            LocalSSHKeyProviderRegistration(),
        )
}