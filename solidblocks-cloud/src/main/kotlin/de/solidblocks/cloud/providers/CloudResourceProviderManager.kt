package de.solidblocks.cloud.providers

interface CloudResourceProviderManager<C : CloudResourceProviderConfiguration, R : ProviderConfigurationRuntime> : ProviderManager<C, R>
