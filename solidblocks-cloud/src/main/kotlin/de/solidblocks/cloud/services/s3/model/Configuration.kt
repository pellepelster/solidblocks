package de.solidblocks.cloud.services.s3.model

import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.ServiceCommonConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class S3ServiceConfiguration(
    override val common: ServiceCommonConfig,
    val backup: BackupConfig,
    val buckets: List<S3ServiceBucketConfiguration>,
) : ServiceConfiguration {
    override val type = "s3"
}

data class S3ServiceBucketAccessKeyConfiguration(
    val name: String,
    val owner: Boolean,
    val read: Boolean,
    val write: Boolean,
)

data class S3ServiceBucketConfiguration(val name: String, val publicAccess: Boolean, val accessKeys: List<S3ServiceBucketAccessKeyConfiguration>, val publicAccessDomains: List<String>)
