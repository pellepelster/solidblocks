package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderConfigurtionRuntime

interface SSHKeyProviderConfigurationManager<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurtionRuntime,
> : ProviderConfigurationManager<C, R>
