package de.solidblocks.cloud.providers.types.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderRegistration

interface SSHKeyProviderRegistration<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurationRuntime,
    M : SSHKeyProviderManager<C, R>,
    > : ProviderRegistration<C, R, M>
