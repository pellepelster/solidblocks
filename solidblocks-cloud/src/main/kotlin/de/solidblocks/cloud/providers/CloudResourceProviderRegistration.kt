package de.solidblocks.cloud.providers

interface CloudResourceProviderRegistration<
    C : CloudResourceProviderConfiguration,
    R : ProviderConfigurtionRuntime,
    M : CloudResourceProviderConfigurationManager<C, R>,
> : ProviderRegistration<C, R, M>
