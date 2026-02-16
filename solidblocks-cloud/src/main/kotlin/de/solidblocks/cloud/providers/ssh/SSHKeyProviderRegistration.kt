package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.ProviderRuntime

interface SSHKeyProviderRegistration<
    C : SSHKeyProviderConfiguration,
    R : ProviderRuntime,
    M : SSHKeyProviderConfigurationManager<C, R>,
> : ProviderRegistration<C, R, M>
