package de.solidblocks.provisioner.vault.provider

import de.solidblocks.api.resources.infrastructure.IInfrastructureClientProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.URI

@Component
class VaultClientProvider :
        IInfrastructureClientProvider<VaultTemplate> {

    private val logger = KotlinLogging.logger {}

    override fun createClient(): VaultTemplate {
        return VaultTemplate(
                VaultEndpoint.from(URI.create("address")),
                TokenAuthentication("XXXX")
        )
    }

    override fun providerType(): Class<VaultTemplate> {
        return VaultTemplate::class.java
    }
}
