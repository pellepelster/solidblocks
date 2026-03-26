package de.solidblocks.cloud.providers

interface CloudResourceProviderConfigurationManager<
    C : CloudResourceProviderConfiguration,
    R : ProviderConfigurtionRuntime,
> : ProviderConfigurationManager<C, R>
