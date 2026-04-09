package de.solidblocks.cloud.services.s3.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.InstanceRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class S3ServiceBucketConfigurationRuntime(
    val name: String,
    val publicAccess: Boolean,
    val accessKeys: List<S3ServiceBucketAccessKeyConfigurationRuntime>,
    val managedPublicWebAccessDomains: Map<String, String>,
    val manuallyManagedPublicWebAccessDomains: Set<String>,
)

data class S3ServiceBucketAccessKeyConfigurationRuntime(val name: String)

data class S3ServiceConfigurationRuntime(override val index: Int, override val name: String, val instance: InstanceRuntime, val backup: BackupRuntime, val buckets: List<S3ServiceBucketConfigurationRuntime>) :
    ServiceConfigurationRuntime
