package de.solidblocks.cloud.providers

interface CloudResourceProviderRegistration<
    C : CloudResourceProviderConfiguration,
    R : ProviderConfigurationRuntime,
    M : CloudResourceProviderConfigurationManager<C, R>,
> : ProviderRegistration<C, R, M>
