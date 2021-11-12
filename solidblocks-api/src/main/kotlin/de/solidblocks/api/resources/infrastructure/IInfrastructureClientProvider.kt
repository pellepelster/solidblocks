package de.solidblocks.api.resources.infrastructure

interface IInfrastructureClientProvider<ClientType> {
    fun createClient(): ClientType

    fun providerType(): Class<ClientType>
}
