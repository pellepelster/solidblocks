package de.solidblocks.cloud.services.s3.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.ServiceCommonRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class S3ServiceBucketConfigurationRuntime(
    val name: String,
    val publicAccess: Boolean,
    val accessKeys: List<S3ServiceBucketAccessKeyConfigurationRuntime>,
    val managedPublicWebAccessDomains: Map<String, String>,
    val manuallyManagedPublicWebAccessDomains: Set<String>,
)

data class S3ServiceBucketAccessKeyConfigurationRuntime(val name: String, val owner: Boolean, val read: Boolean, val write: Boolean)

data class S3ServiceConfigurationRuntime(
    override val index: Int,
    override val common: ServiceCommonRuntime,
    override val backup: BackupRuntime,
    val buckets: List<S3ServiceBucketConfigurationRuntime>,
) :
    ServiceConfigurationRuntime
