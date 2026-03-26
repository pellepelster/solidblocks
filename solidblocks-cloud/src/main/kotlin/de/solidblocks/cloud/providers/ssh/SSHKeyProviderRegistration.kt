package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurtionRuntime
import de.solidblocks.cloud.providers.ProviderRegistration

interface SSHKeyProviderRegistration<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurtionRuntime,
    M : SSHKeyProviderConfigurationManager<C, R>,
> : ProviderRegistration<C, R, M>
