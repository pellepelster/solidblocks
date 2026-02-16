package de.solidblocks.cloud.providers

interface CloudResourceProviderConfigurationManager<
    C : CloudResourceProviderConfiguration,
    R : ProviderRuntime,
> : ProviderConfigurationManager<C, R>
