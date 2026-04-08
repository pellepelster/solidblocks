package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime

interface SSHKeyProviderConfigurationManager<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurationRuntime,
> : ProviderConfigurationManager<C, R>
