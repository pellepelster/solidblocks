package de.solidblocks.cloud.providers.backup.aws

import de.solidblocks.cloud.providers.types.backup.BackupProviderConfigurationRuntime

data class S3BackupProviderConfigurationRuntime(val region: String, val accessKey: String, val secretKey: String) : BackupProviderConfigurationRuntime
