package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderRegistration

interface SSHKeyProviderRegistration<
    C : SSHKeyProviderConfiguration,
    R : ProviderConfigurationRuntime,
    M : SSHKeyProviderConfigurationManager<C, R>,
> : ProviderRegistration<C, R, M>
