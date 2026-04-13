package de.solidblocks.cloud.providers

interface CloudResourceProviderRegistration<C : CloudResourceProviderConfiguration, R : ProviderConfigurationRuntime, M : CloudResourceProviderManager<C, R>> : ProviderRegistration<C, R, M>
