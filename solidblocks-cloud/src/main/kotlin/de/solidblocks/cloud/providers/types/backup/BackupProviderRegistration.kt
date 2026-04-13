package de.solidblocks.cloud.providers.types.backup

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderRegistration

interface BackupProviderRegistration<
    C : BackupProviderConfiguration,
    R : ProviderConfigurationRuntime,
    M : BackupProviderManager<C, R>,
    > : ProviderRegistration<C, R, M>
