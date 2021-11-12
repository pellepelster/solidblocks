package de.solidblocks.provisioner.fixtures

import de.solidblocks.api.resources.infrastructure.IInfrastructureClientProvider

class TestInfrastructureClientProvider : IInfrastructureClientProvider<String> {
    override fun createClient(): String {
        return "provider"
    }

    override fun providerType(): Class<String> {
        return String::class.java
    }
}
