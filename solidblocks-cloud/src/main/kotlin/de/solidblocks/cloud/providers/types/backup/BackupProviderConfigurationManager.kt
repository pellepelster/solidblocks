package de.solidblocks.cloud.providers.types.backup

import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime

interface BackupProviderConfigurationManager<
    C : BackupProviderConfiguration,
    R : ProviderConfigurationRuntime,
    > : ProviderConfigurationManager<C, R>
