package de.solidblocks.cloud.providers.types.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime

interface SSHKeyProviderConfigurationManager<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurationRuntime,
    > : ProviderConfigurationManager<C, R>
