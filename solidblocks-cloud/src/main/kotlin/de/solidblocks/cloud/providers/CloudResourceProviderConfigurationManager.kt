package de.solidblocks.cloud.providers

interface CloudResourceProviderConfigurationManager<
    C : CloudResourceProviderConfiguration,
    R : ProviderConfigurationRuntime,
> : ProviderConfigurationManager<C, R>
