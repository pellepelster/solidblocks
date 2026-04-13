package de.solidblocks.cloud.providers.types.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager

interface SSHKeyProviderManager<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurationRuntime,
    > : ProviderManager<C, R>
