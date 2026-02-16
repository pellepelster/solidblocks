package de.solidblocks.cloud.providers

interface CloudResourceProviderRegistration<
    C : CloudResourceProviderConfiguration,
    R : ProviderRuntime,
    M : CloudResourceProviderConfigurationManager<C, R>,
> : ProviderRegistration<C, R, M>
