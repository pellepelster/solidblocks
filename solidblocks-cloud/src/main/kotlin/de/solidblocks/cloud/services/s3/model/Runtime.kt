package de.solidblocks.cloud.services.s3.model

import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class S3ServiceBucketConfigurationRuntime(val name: String, val publicAccess: Boolean, val accessKeys: List<S3ServiceBucketAccessKeyConfigurationRuntime>)

data class S3ServiceBucketAccessKeyConfigurationRuntime(val name: String)

data class S3ServiceConfigurationRuntime(
    override val name: String,
    val buckets: List<S3ServiceBucketConfigurationRuntime>,
) : ServiceConfigurationRuntime
