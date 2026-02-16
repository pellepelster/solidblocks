package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderRuntime

interface SSHKeyProviderConfigurationManager<C : SSHKeyProviderConfiguration, R : ProviderRuntime> :
    ProviderConfigurationManager<C, R>
