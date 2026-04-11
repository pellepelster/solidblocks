package de.solidblocks.cloud.providers.backup

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime

data class S3BackupProviderRuntime(val region: String, val accessKey: String, val secretKey: String) : ProviderConfigurationRuntime
